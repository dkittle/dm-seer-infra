package ca.kittle.cluster

import ca.kittle.Stack
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ec2.kotlin.Subnet
import com.pulumi.aws.ecs.kotlin.Cluster
import com.pulumi.aws.ecs.kotlin.Service
import com.pulumi.aws.ecs.kotlin.TaskDefinition
import com.pulumi.aws.ecs.kotlin.service
import com.pulumi.aws.alb.kotlin.LoadBalancer
import com.pulumi.aws.lb.kotlin.TargetGroup
import com.pulumi.core.Output

suspend fun createService(env: Stack, cluster: Cluster, taskDefinition: TaskDefinition, ecsSecurityGroup: SecurityGroup, subnet1Id: Output<String>, subnet2Id: Output<String>, elb: LoadBalancer, targetGroup: TargetGroup): Service {
    val clusterArn = cluster.arn.applyValue(fun(arn: String): String { return arn })
    val taskDefinitionArn = taskDefinition.arn.applyValue(fun(arn: String): String { return arn })
    val ecsSecurityGroupId = ecsSecurityGroup.id.applyValue(fun(name: String): String { return name })
    val elbName = elb.name.applyValue(fun(name: String): String { return name })
    val targetGroupArn = targetGroup.arn.applyValue(fun(name: String): String { return name })
//    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
//    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })

    return service("${env.name.lowercase()}-dmseer-service") {
        args {
            cluster(clusterArn)
            taskDefinition(taskDefinitionArn)
            desiredCount(1)
            launchType("FARGATE")
            platformVersion("LATEST")
            loadBalancers {
                containerName("dmseer-app-${env.stackName}")
                containerPort(8081)
                targetGroupArn(targetGroupArn)
            }
            networkConfiguration {
                assignPublicIp(true)
                securityGroups(ecsSecurityGroupId)
                subnets(subnet1Id, subnet2Id)
            }
        }
        opts {
            dependsOn(elb, targetGroup)
        }
    }
}