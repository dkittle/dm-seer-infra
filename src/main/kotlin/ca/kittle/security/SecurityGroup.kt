package ca.kittle.security

import ca.kittle.Stack
import ca.kittle.envTags
import ca.kittle.network.vpcCidr
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ec2.kotlin.Vpc
import com.pulumi.aws.ec2.kotlin.securityGroup
import com.pulumi.aws.vpc.kotlin.securityGroupEgressRule
import com.pulumi.aws.vpc.kotlin.securityGroupIngressRule
import com.pulumi.core.Output

suspend fun createMongoSecurityGroup(env: Stack, vpcId: Output<String>): SecurityGroup {
//    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    val sg = securityGroup("${env.stackName}-mongo-security-group") {
        args {
            vpcId(vpcId)
            tags(envTags(env, "${env.stackName}-mongo-security-group"))
        }
    }
    val sgId = sg.id.applyValue(fun(name: String): String { return name })
    securityGroupIngressRule("${env.stackName}-mongo-ingress-http") {
        args {
            securityGroupId(sgId)
            cidrIpv4(vpcCidr(env))
            fromPort(27017)
            ipProtocol("tcp")
            toPort(27017)
        }
    }
    securityGroupEgressRule("${env.stackName}-mongo-egress-all") {
        args {
            securityGroupId(sgId)
//            fromPort(0)
//            toPort(0)
            cidrIpv4("0.0.0.0/0")
            ipProtocol("-1")
        }
    }
    return sg
}

suspend fun createEcsSecurityGroup(env: Stack, vpcId: Output<String>, mongoSG: SecurityGroup): SecurityGroup {
    val mongoSGId = mongoSG.id.applyValue(fun(name: String): String { return name })
    val sg = securityGroup("${env.stackName}-ecs-security-group") {
        args {
            description("Allow all inbound HTTP(S) traffic, and any outbound traffic")
            vpcId(vpcId)
            tags(envTags(env, "${env.stackName}-ecs-security-group"))
        }
    }
    val sgId = sg.id.applyValue(fun(name: String): String { return name })
    securityGroupIngressRule("${env.stackName}-ecs-ingress-http") {
        args {
            securityGroupId(sgId)
            cidrIpv4(vpcCidr(env))
            fromPort(8081)
            ipProtocol("tcp")
            toPort(8081)
        }
    }
    securityGroupEgressRule("${env.stackName}-ecs-egress-mongo") {
        args {
            securityGroupId(sgId)
            fromPort(27017)
            toPort(27017)
            ipProtocol("tcp")
            referencedSecurityGroupId(mongoSGId)
        }
    }
    securityGroupEgressRule("${env.stackName}-ecs-egress-all") {
        args {
            securityGroupId(sgId)
            cidrIpv4("0.0.0.0/0")
            ipProtocol("-1")
        }
    }
    return sg
}

suspend fun createElbSecurityGroup(env: Stack, vpcId: Output<String>): SecurityGroup {
//    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    val sg = securityGroup("${env.stackName}-elb-security-group") {
        args {
            vpcId(vpcId)
            tags(envTags(env, "${env.stackName}-elb-security-group"))
        }
    }
    val sgId = sg.id.applyValue(fun(name: String): String { return name })
//    securityGroupIngressRule("${env.stackName}-ecs-ingress-ssh-bloomfield") {
//        args {
//            securityGroupId(sgId)
//            cidrIpv4("24.156.192.0/18")
//            fromPort(22)
//            ipProtocol("tcp")
//            toPort(22)
//        }
//    }
//    securityGroupIngressRule("${env.stackName}-ecs-ingress-ssh-roncy") {
//        args {
//            securityGroupId(sgId)
//            cidrIpv4("173.32.0.0/14")
//            fromPort(22)
//            ipProtocol("tcp")
//            toPort(22)
//        }
//    }
    securityGroupIngressRule("${env.stackName}-ecs-ingress-ipv4http") {
        args {
            securityGroupId(sgId)
            cidrIpv4("0.0.0.0/0")
            fromPort(80)
            ipProtocol("tcp")
            toPort(80)
        }
    }
    securityGroupIngressRule("${env.stackName}-ecs-ingress-ipv4https") {
        args {
            securityGroupId(sgId)
            cidrIpv4("0.0.0.0/0")
            fromPort(443)
            ipProtocol("tcp")
            toPort(443)
        }
    }
    securityGroupIngressRule("${env.stackName}-ecs-ingress-ipv6http") {
        args {
            securityGroupId(sgId)
            cidrIpv6("::/0")
            fromPort(80)
            ipProtocol("tcp")
            toPort(80)
        }
    }
    securityGroupIngressRule("${env.stackName}-ecs-ingress-ipv6https") {
        args {
            securityGroupId(sgId)
            cidrIpv6("::/0")
            fromPort(443)
            ipProtocol("tcp")
            toPort(443)
        }
    }
    securityGroupEgressRule("${env.stackName}-ecs-egress-http") {
        args {
            securityGroupId(sgId)
            cidrIpv4(vpcCidr(env))
            fromPort(8081)
            ipProtocol("tcp")
            toPort(8081)
        }
    }

    return sg
}