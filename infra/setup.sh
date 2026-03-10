#!/bin/bash
set -e

# ===========================
# CliniQ AWS Infrastructure Setup
# Run this ONCE from your Mac (requires AWS CLI configured)
# Region: us-east-1
# ===========================

REGION="us-east-1"
PROJECT="cliniq"
DB_NAME="msc"
DB_USERNAME="cliniqadmin"
DB_PASSWORD="postgres"  # CHANGE THIS before running
JWT_SECRET=$(openssl rand -base64 32)

echo "============================================"
echo "  CliniQ AWS Infrastructure Setup"
echo "  Region: $REGION"
echo "============================================"

# -------------------------------------------------
# Step 1: Create ECR Repository
# -------------------------------------------------
echo ""
echo "[1/8] Creating ECR repository..."
aws ecr create-repository \
  --repository-name ${PROJECT}-backend \
  --region $REGION \
  --image-scanning-configuration scanOnPush=true \
  --output text --query 'repository.repositoryUri' 2>/dev/null || \
  aws ecr describe-repositories \
    --repository-names ${PROJECT}-backend \
    --region $REGION \
    --output text --query 'repositories[0].repositoryUri'

ECR_URI=$(aws ecr describe-repositories \
  --repository-names ${PROJECT}-backend \
  --region $REGION \
  --output text --query 'repositories[0].repositoryUri')
echo "ECR URI: $ECR_URI"

# -------------------------------------------------
# Step 2: Get Default VPC and Subnets
# -------------------------------------------------
echo ""
echo "[2/8] Getting default VPC and subnets..."
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=isDefault,Values=true" \
  --region $REGION \
  --output text --query 'Vpcs[0].VpcId')
echo "VPC: $VPC_ID"

SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --region $REGION \
  --output text --query 'Subnets[*].SubnetId' | tr '\t' ',')
echo "Subnets: $SUBNET_IDS"

# Pick first two subnets for ALB (needs at least 2 AZs)
SUBNET_1=$(echo $SUBNET_IDS | cut -d',' -f1)
SUBNET_2=$(echo $SUBNET_IDS | cut -d',' -f2)

# -------------------------------------------------
# Step 3: Create Security Groups
# -------------------------------------------------
echo ""
echo "[3/8] Creating security groups..."

# ALB Security Group (allows HTTP from internet)
ALB_SG=$(aws ec2 create-security-group \
  --group-name ${PROJECT}-alb-sg \
  --description "CliniQ ALB - allows HTTP from internet" \
  --vpc-id $VPC_ID \
  --region $REGION \
  --output text --query 'GroupId' 2>/dev/null || \
  aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${PROJECT}-alb-sg" "Name=vpc-id,Values=$VPC_ID" \
    --region $REGION \
    --output text --query 'SecurityGroups[0].GroupId')
echo "ALB SG: $ALB_SG"

aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG \
  --protocol tcp --port 80 --cidr 0.0.0.0/0 \
  --region $REGION 2>/dev/null || true

# ECS Security Group (allows 8080 from ALB)
ECS_SG=$(aws ec2 create-security-group \
  --group-name ${PROJECT}-ecs-sg \
  --description "CliniQ ECS - allows 8080 from ALB" \
  --vpc-id $VPC_ID \
  --region $REGION \
  --output text --query 'GroupId' 2>/dev/null || \
  aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${PROJECT}-ecs-sg" "Name=vpc-id,Values=$VPC_ID" \
    --region $REGION \
    --output text --query 'SecurityGroups[0].GroupId')
echo "ECS SG: $ECS_SG"

aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG \
  --protocol tcp --port 8080 --source-group $ALB_SG \
  --region $REGION 2>/dev/null || true

# RDS Security Group (allows 5432 from ECS)
RDS_SG=$(aws ec2 create-security-group \
  --group-name ${PROJECT}-rds-sg \
  --description "CliniQ RDS - allows 5432 from ECS" \
  --vpc-id $VPC_ID \
  --region $REGION \
  --output text --query 'GroupId' 2>/dev/null || \
  aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${PROJECT}-rds-sg" "Name=vpc-id,Values=$VPC_ID" \
    --region $REGION \
    --output text --query 'SecurityGroups[0].GroupId')
echo "RDS SG: $RDS_SG"

aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG \
  --protocol tcp --port 5432 --source-group $ECS_SG \
  --region $REGION 2>/dev/null || true

# Allow your local machine to access RDS (for running schema.sql)
echo "Adding your public IP to RDS security group for schema setup..."
MY_IP=$(curl -s https://checkip.amazonaws.com)
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG \
  --protocol tcp --port 5432 --cidr ${MY_IP}/32 \
  --region $REGION 2>/dev/null || true
echo "Your IP ($MY_IP) added to RDS SG"

# -------------------------------------------------
# Step 4: Create RDS PostgreSQL Instance
# -------------------------------------------------
echo ""
echo "[4/8] Creating RDS PostgreSQL instance (this takes 5-10 minutes)..."
aws rds create-db-instance \
  --db-instance-identifier ${PROJECT}-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15 \
  --master-username $DB_USERNAME \
  --master-user-password $DB_PASSWORD \
  --allocated-storage 20 \
  --db-name $DB_NAME \
  --vpc-security-group-ids $RDS_SG \
  --publicly-accessible \
  --backup-retention-period 7 \
  --no-multi-az \
  --storage-type gp2 \
  --region $REGION \
  --output text --query 'DBInstance.DBInstanceIdentifier' 2>/dev/null || echo "RDS instance already exists"

echo "Waiting for RDS to become available..."
aws rds wait db-instance-available \
  --db-instance-identifier ${PROJECT}-db \
  --region $REGION

RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier ${PROJECT}-db \
  --region $REGION \
  --output text --query 'DBInstances[0].Endpoint.Address')
echo "RDS Endpoint: $RDS_ENDPOINT"

# -------------------------------------------------
# Step 5: Create Application Load Balancer
# -------------------------------------------------
echo ""
echo "[5/8] Creating Application Load Balancer..."
ALB_ARN=$(aws elbv2 create-load-balancer \
  --name ${PROJECT}-alb \
  --subnets $SUBNET_1 $SUBNET_2 \
  --security-groups $ALB_SG \
  --scheme internet-facing \
  --type application \
  --region $REGION \
  --output text --query 'LoadBalancers[0].LoadBalancerArn' 2>/dev/null || \
  aws elbv2 describe-load-balancers \
    --names ${PROJECT}-alb \
    --region $REGION \
    --output text --query 'LoadBalancers[0].LoadBalancerArn')
echo "ALB ARN: $ALB_ARN"

ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names ${PROJECT}-alb \
  --region $REGION \
  --output text --query 'LoadBalancers[0].DNSName')
echo "ALB DNS (your public URL): http://$ALB_DNS"

# Create Target Group
TG_ARN=$(aws elbv2 create-target-group \
  --name ${PROJECT}-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /api/queue/display \
  --health-check-interval-seconds 30 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3 \
  --region $REGION \
  --output text --query 'TargetGroups[0].TargetGroupArn' 2>/dev/null || \
  aws elbv2 describe-target-groups \
    --names ${PROJECT}-tg \
    --region $REGION \
    --output text --query 'TargetGroups[0].TargetGroupArn')
echo "Target Group: $TG_ARN"

# Create Listener
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=$TG_ARN \
  --region $REGION \
  --output text --query 'Listeners[0].ListenerArn' 2>/dev/null || true

# -------------------------------------------------
# Step 6: Create ECS Cluster
# -------------------------------------------------
echo ""
echo "[6/8] Creating ECS cluster..."
aws ecs create-cluster \
  --cluster-name ${PROJECT}-cluster \
  --region $REGION \
  --output text --query 'cluster.clusterArn' 2>/dev/null || echo "Cluster already exists"

# -------------------------------------------------
# Step 7: Create IAM Roles for ECS
# -------------------------------------------------
echo ""
echo "[7/8] Creating IAM roles..."

# ECS Task Execution Role
aws iam create-role \
  --role-name ${PROJECT}-ecs-execution-role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }' 2>/dev/null || true

aws iam attach-role-policy \
  --role-name ${PROJECT}-ecs-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy 2>/dev/null || true

# Get AWS Account ID
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --output text)

# -------------------------------------------------
# Step 8: Store secrets in SSM Parameter Store
# -------------------------------------------------
echo ""
echo "[8/8] Storing secrets in SSM Parameter Store..."
aws ssm put-parameter \
  --name "/${PROJECT}/db-password" \
  --value "$DB_PASSWORD" \
  --type SecureString \
  --overwrite \
  --region $REGION || true

aws ssm put-parameter \
  --name "/${PROJECT}/jwt-secret" \
  --value "$JWT_SECRET" \
  --overwrite \
  --type SecureString \
  --region $REGION || true

aws ssm put-parameter \
  --name "/${PROJECT}/db-url" \
  --value "jdbc:postgresql://${RDS_ENDPOINT}:5432/${DB_NAME}?currentSchema=cliniq" \
  --type String \
  --overwrite \
  --region $REGION || true

aws ssm put-parameter \
  --name "/${PROJECT}/db-username" \
  --value "$DB_USERNAME" \
  --type String \
  --overwrite \
  --region $REGION || true

# Add SSM read permissions to execution role
aws iam put-role-policy \
  --role-name ${PROJECT}-ecs-execution-role \
  --policy-name ${PROJECT}-ssm-access \
  --policy-document "{
    \"Version\": \"2012-10-17\",
    \"Statement\": [{
      \"Effect\": \"Allow\",
      \"Action\": [
        \"ssm:GetParameters\",
        \"ssm:GetParameter\"
      ],
      \"Resource\": \"arn:aws:ssm:${REGION}:${AWS_ACCOUNT_ID}:parameter/${PROJECT}/*\"
    }]
  }" 2>/dev/null || true

# -------------------------------------------------
# Summary
# -------------------------------------------------
echo ""
echo "============================================"
echo "  SETUP COMPLETE"
echo "============================================"
echo ""
echo "ECR Repository:  $ECR_URI"
echo "RDS Endpoint:    $RDS_ENDPOINT"
echo "ALB Public URL:  http://$ALB_DNS"
echo "ECS Cluster:     ${PROJECT}-cluster"
echo "AWS Account ID:  $AWS_ACCOUNT_ID"
echo ""
echo "============================================"
echo "  NEXT STEPS"
echo "============================================"
echo ""
echo "1. Run schema.sql on RDS:"
echo "   psql -h $RDS_ENDPOINT -U $DB_USERNAME -d $DB_NAME -f schema.sql"
echo "   (password: $DB_PASSWORD)"
echo ""
echo "2. Add these GitHub Secrets to your repo:"
echo "   AWS_ACCESS_KEY_ID       = (your AWS access key)"
echo "   AWS_SECRET_ACCESS_KEY   = (your AWS secret key)"
echo "   AWS_ACCOUNT_ID          = $AWS_ACCOUNT_ID"
echo "   AWS_REGION              = $REGION"
echo ""
echo "3. Push to main branch to trigger deployment"
echo ""
echo "JWT_SECRET generated: $JWT_SECRET"
echo "(stored in SSM Parameter Store)"
echo "============================================"
