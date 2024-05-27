package ca.kittle.security

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ec2.kotlin.Vpc
import com.pulumi.aws.ec2.kotlin.securityGroup

data class SecurityGroupContext(
    val env: Stack,
    val name: String,
    val protocol: String,
    val port: Int,
    val vpc: Vpc
)

suspend fun inboundSecurityGroup(context: SecurityGroupContext) =
    securityGroup("${context.name}-security-group") {
        args {
            vpcId(context.vpc.id)
            ingress {
                fromPort(context.port)
                toPort(context.port)
                protocol(context.protocol)
                cidrBlocks(context.vpc.cidrBlock)
            }
            tags(envTags(context.env, "${context.name}-sg"))
        }
    }