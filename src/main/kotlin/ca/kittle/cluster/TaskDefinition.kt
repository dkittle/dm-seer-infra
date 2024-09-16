package ca.kittle.cluster

import ca.kittle.Stack
import com.pulumi.aws.ecr.kotlin.Repository
import com.pulumi.aws.ecs.kotlin.TaskDefinition
import com.pulumi.aws.ecs.kotlin.taskDefinition
import com.pulumi.aws.iam.kotlin.Role

suspend fun createTaskDefinition(env: Stack, taskExecutionRole: Role): TaskDefinition {
    val roleArn = taskExecutionRole.arn.applyValue(fun(arn: String): String { return arn })
    return taskDefinition("${env.stackName}-task-definition") {
        args {
            family("ecs-dmseer-family")
            networkMode("awsvpc")
            executionRoleArn(roleArn)
            containerDefinitions("""
                [
                    {
                        "name": "dmseer-app-${env.stackName}",
                        "image": "814245790557.dkr.ecr.ca-central-1.amazonaws.com/prod-dmseer-repository-599273a/latest",
                        "memory": 800,
                        "portMappings": [
                            {
                                "containerPort": 80,
                                "hostPort": 80
                            }
                        ],
                        "secrets": [{
                            "name": "MONGO_HOST",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/mongodb/host"                        
                        },{
                            "name": "MONGO_USER",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/mongodb/username"                        
                        },{
                            "name": "MONGO_PASSWORD",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:secret/shared-config/${env.stackName}/mongodb/password"                        
                        },{
                            "name": "TRUSTSTORE_PASSWORD",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:secret/shared-config/${env.stackName}/mongodb/truststore_password"                        
                        }]
                    }
                ]
                """.trimIndent())
        }
    }
}

suspend fun createFGTaskDefinition(env: Stack, taskExecutionRole: Role): TaskDefinition {
    val roleArn = taskExecutionRole.arn.applyValue(fun(arn: String): String { return arn })
    return taskDefinition("${env.stackName}-task-definition") {
        args {
            family("ecs-dmseer-family")
            networkMode("awsvpc")
            requiresCompatibilities("FARGATE")
            taskRoleArn(roleArn)
            executionRoleArn(roleArn)
            cpu("512")
            memory("1024")
            runtimePlatform {
                cpuArchitecture("X86_64")
                operatingSystemFamily("LINUX")
            }
            containerDefinitions("""
                [
                    {
                        "name": "dmseer-app-${env.stackName}",
                        "image": "814245790557.dkr.ecr.ca-central-1.amazonaws.com/prod-container-repository-03574f0",
                        "cpu": 512,
                        "memory": 960,
                        "logConfiguration": {
                            "logDriver": "awslogs",
                            "options": {
                                "awslogs-create-group": "true",
                                "awslogs-group": "ecs-dmseer",
                                "awslogs-region": "ca-central-1",
                                "awslogs-stream-prefix": "ecs"
                            }
                        },
                        "portMappings": [
                            {
                                "containerPort": 8081,
                                "hostPort": 8081
                            }
                        ],
                        "essential": true,
                        "healthCheck": {
                            "retries": 3,
                            "command": [
                              "\"CMD-SHELL\"",
                              "\"curl -f http://localhost:8081/ || exit 1\""
                            ],
                            "timeout": 5,
                            "interval": 30,
                            "startPeriod": 30
                        },
                        "secrets": [{
                            "name": "MONGO_HOST",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/mongodb/host"                        
                        },{
                            "name": "MONGO_USER",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/mongodb/username"                        
                        },{
                            "name": "MONGO_PASSWORD",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/mongodb/password"                        
                        },{
                            "name": "TRUSTSTORE_PASSWORD",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/mongodb/truststore_password"                        
                        },{
                            "name": "ADMIN_USERNAME",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/dmseer/admin_username"                        
                        },{
                            "name": "ADMIN_PASSWORD",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/dmseer/admin_password"                        
                        },{
                            "name": "SEER_BOT_TOKEN",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/dmseer/discord_bot_token"                        
                        },{
                            "name": "JWT_SECRET",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/dmseer/jwt_secret"                        
                        },{
                            "name": "POSTMARK_API_KEY",
                            "valueFrom": "arn:aws:ssm:ca-central-1:814245790557:parameter/shared-config/${env.stackName}/dmseer/postman_apikey"                        
                        }]
                    }
                ]
                """.trimIndent())
        }
//        opts {
//            dependsOn(repository)
//        }
    }
}