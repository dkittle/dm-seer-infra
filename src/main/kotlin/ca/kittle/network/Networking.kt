package ca.kittle.network

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ec2.kotlin.Vpc
import com.pulumi.aws.ec2.kotlin.securityGroup
import com.pulumi.aws.ec2.kotlin.subnet
import com.pulumi.aws.ec2.kotlin.vpc


private fun vpcCidr(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.0.0/16"
        Stack.Staging -> "10.12.0.0/16"
        Stack.Prod -> "10.16.0.0/16"
    }

private fun privateCidr(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.20.0/24"
        Stack.Staging -> "10.12.20.0/24"
        Stack.Prod -> "10.16.20.0/24"
    }

suspend fun environmentVpc(env: Stack) = vpc("${env.name.lowercase()}_vpc") {
    args {
        cidrBlock(vpcCidr(env))
        enableDnsHostnames(true)
        enableDnsSupport(true)
        tags(envTags(env, "vpc"))
    }
}

suspend fun privateSubnet(env: Stack, vpc: Vpc) = subnet("${env.name.lowercase()}_private_subnet") {
    args {
        vpcId(vpc.id)
        cidrBlock(privateCidr(env))
        mapPublicIpOnLaunch(false)
        tags(envTags(env, "private-subnet"))
    }
}

suspend fun natSecurityGroup(env: Stack) = securityGroup("${env.name.lowercase()}_nat_server_sg") {
    args {
        tags(envTags(env, "nat-security-group"))
    }
}
