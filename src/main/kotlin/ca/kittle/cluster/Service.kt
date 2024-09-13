package ca.kittle.cluster

import ca.kittle.Stack
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ec2.kotlin.Subnet
import com.pulumi.aws.ecs.kotlin.Cluster
import com.pulumi.aws.ecs.kotlin.Service
import com.pulumi.aws.ecs.kotlin.TaskDefinition
import com.pulumi.aws.ecs.kotlin.service

suspend fun createService(env: Stack, cluster: Cluster, taskDefinition: TaskDefinition, ecsSecurityGroup: SecurityGroup, subnet1: Subnet, subnet2: Subnet): Service {
    val clusterId = cluster.arn.applyValue(fun(arn: String): String { return arn })
    val taskDefinitionArn = taskDefinition.arn.applyValue(fun(arn: String): String { return arn })
    val ecsSecurityGroupId = ecsSecurityGroup.id.applyValue(fun(name: String): String { return name })
    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })

    return service("${env.name.lowercase()}-dmseer-service") {
        args {
            cluster(clusterId)
            taskDefinition(taskDefinitionArn)
            desiredCount(2)
            launchType("EC2")
            networkConfiguration {
                securityGroups(ecsSecurityGroupId)
                subnets(subnet1Id, subnet2Id)
            }
        }
    }
}