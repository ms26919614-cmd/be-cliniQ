#!/bin/bash
set -e

# ===========================
# CliniQ - Create ECS Service
# Run this AFTER setup.sh and AFTER first Docker image is pushed
# ===========================

REGION="us-east-1"
PROJECT="cliniq"

AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --output text)

echo "============================================"
echo "  Creating ECS Service"
echo "  Account: $AWS_ACCOUNT_ID"
echo "============================================"

# -------------------------------------------------
# Step 1: Update task-definition.json with account ID
# -------------------------------------------------
echo ""
echo "[1/4] Preparing task definition..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
sed "s/ACCOUNT_ID/$AWS_ACCOUNT_ID/g" "$SCRIPT_DIR/task-definition.json" > /tmp/cliniq-task-def.json

# -------------------------------------------------
# Step 2: Create CloudWatch Log Group
# -------------------------------------------------
echo ""
echo "[2/4] Creating CloudWatch log group..."
aws logs create-log-group \
  --log-group-name /ecs/cliniq-backend \
  --region $REGION 2>/dev/null || true

# -------------------------------------------------
# Step 3: Register Task Definition
# -------------------------------------------------
echo ""
echo "[3/4] Registering task definition..."
TASK_DEF_ARN=$(aws ecs register-task-definition \
  --cli-input-json file:///tmp/cliniq-task-def.json \
  --region $REGION \
  --output text --query 'taskDefinition.taskDefinitionArn')
echo "Task Definition: $TASK_DEF_ARN"

# -------------------------------------------------
# Step 4: Create ECS Service
# -------------------------------------------------
echo ""
echo "[4/4] Creating ECS service..."

# Get VPC resources
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=isDefault,Values=true" \
  --region $REGION \
  --output text --query 'Vpcs[0].VpcId')

ECS_SG=$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=${PROJECT}-ecs-sg" "Name=vpc-id,Values=$VPC_ID" \
  --region $REGION \
  --output text --query 'SecurityGroups[0].GroupId')

SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --region $REGION \
  --output text --query 'Subnets[*].SubnetId')
SUBNET_1=$(echo $SUBNET_IDS | awk '{print $1}')
SUBNET_2=$(echo $SUBNET_IDS | awk '{print $2}')

TG_ARN=$(aws elbv2 describe-target-groups \
  --names ${PROJECT}-tg \
  --region $REGION \
  --output text --query 'TargetGroups[0].TargetGroupArn')

aws ecs create-service \
  --cluster ${PROJECT}-cluster \
  --service-name ${PROJECT}-service \
  --task-definition cliniq-backend \
  --desired-count 1 \
  --launch-type FARGATE \
  --platform-version LATEST \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_1,$SUBNET_2],securityGroups=[$ECS_SG],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=$TG_ARN,containerName=cliniq-backend,containerPort=8080" \
  --region $REGION \
  --output text --query 'service.serviceArn'

echo ""
echo "============================================"
echo "  ECS Service Created!"
echo "============================================"

ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names ${PROJECT}-alb \
  --region $REGION \
  --output text --query 'LoadBalancers[0].DNSName')

echo ""
echo "Your API will be available at:"
echo "  http://$ALB_DNS/api/queue/display"
echo ""
echo "Wait 2-3 minutes for the service to stabilize."
echo "============================================"
