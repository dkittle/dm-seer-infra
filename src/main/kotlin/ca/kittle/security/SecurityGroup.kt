package ca.kittle.security

import ca.kittle.Stack
import ca.kittle.envTags
import ca.kittle.network.vpcCidr
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ec2.kotlin.Subnet
import com.pulumi.aws.ec2.kotlin.Vpc
import com.pulumi.aws.ec2.kotlin.securityGroup
import com.pulumi.aws.vpc.kotlin.securityGroupEgressRule
import com.pulumi.aws.vpc.kotlin.securityGroupIngressRule

data class SecurityGroupContext(
    val env: Stack,
    val name: String,
    val protocol: String,
    val port: Int,
    val vpc: Vpc
)

suspend fun createMongoSecurityGroup(context: SecurityGroupContext) =
    securityGroup("${context.env.stackEnv}-mongo-security-group") {
        args {
            vpcId(context.vpc.id)
            ingress {
                fromPort(context.port)
                toPort(context.port)
                protocol(context.protocol)
                cidrBlocks(context.vpc.cidrBlock)
            }
            egress {
                fromPort(0)
                toPort(0)
                protocol("-1")
                cidrBlocks("0.0.0.0/0")
            }
            tags(envTags(context.env, "${context.env.stackEnv}-mongo-security-group"))
        }
    }

suspend fun createEcsSecurityGroup(env: Stack, vpc: Vpc): SecurityGroup {
    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    val sg = securityGroup("${env.stackEnv}-ecs-security-group") {
        args {
            description("Allow all inbound HTTP(S) traffic, and all outbound traffic")
            vpcId(vpcId)
        }
    }
    val sgId = sg.id.applyValue(fun(name: String): String { return name })
    securityGroupIngressRule("${env.stackEnv}-ecs-ingress-http") {
        args {
            securityGroupId(sgId)
            cidrIpv4(vpcCidr(env))
            fromPort(80)
            ipProtocol("tcp")
            toPort(80)
        }
    }
    securityGroupIngressRule("${env.stackEnv}-ecs-ingress-https") {
        args {
            securityGroupId(sgId)
            cidrIpv4(vpcCidr(env))
            fromPort(443)
            ipProtocol("tcp")
            toPort(443)
        }
    }
    securityGroupEgressRule("${env.stackEnv}-ecs-egress-all") {
        args {
            securityGroupId(sgId)
            cidrIpv4("0.0.0.0/0")
            ipProtocol("-1")
        }
    }

    return sg
}