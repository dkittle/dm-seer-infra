package ca.kittle.compute

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.autoscaling.kotlin.Group
import com.pulumi.aws.autoscaling.kotlin.group
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ec2.kotlin.Subnet
import com.pulumi.aws.ec2.kotlin.launchTemplate
import com.pulumi.aws.lb.kotlin.TargetGroup
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
suspend fun createAutoScalingGroup(env: Stack, targetGroup: TargetGroup, ecsSecurityGroup: SecurityGroup, subnet1: Subnet, subnet2: Subnet): Group {
    val securityGroupId = ecsSecurityGroup.id.applyValue(fun(name: String): String { return name })
    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })
    val clusterScript = "#!/bin/bash\necho ECS_CLUSTER=${env.stackEnv}-dmseer-ecs-cluster >> /etc/ecs/ecs.config"
    val userData = Base64.Default.encode(clusterScript.encodeToByteArray())

    val launchTemplate = launchTemplate("${env.stackEnv}-dmseer-launch-template") {
        args {
            namePrefix("${env.stackEnv}-dmseer")
//            imageId("ami-07e35c3920b92d884")
            imageId("ami-08e93cdd76c56b968")
            instanceType("t2.small")
            userData(userData)
            vpcSecurityGroupIds(securityGroupId)
        }
    }
    val launchTemplateId = launchTemplate.id.applyValue(fun(name: String): String { return name })
    val maxSize = if (env == Stack.Prod) 4 else 2
    val desiredSize = maxSize / 2

    val targetGroupArn = targetGroup.arn.applyValue(fun(arn: String): String { return arn })

    return group("${env.name.lowercase()}-dmseer-autoscaling-group") {
        args {
//            availabilityZones("ca-central-1a", "ca-central-1b")
            vpcZoneIdentifiers(subnet1Id, subnet2Id)
            desiredCapacity(desiredSize)
            minSize(1)
            maxSize(maxSize)
            targetGroupArns(targetGroupArn)
            launchTemplate {
                id(launchTemplateId)
                version("\$Latest")
            }
        }
    }
}