package ca.kittle.network

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.docdb.kotlin.SubnetGroup
import com.pulumi.aws.docdb.kotlin.subnetGroup
import com.pulumi.aws.ec2.kotlin.*


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

private fun privateCidr2(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.30.0/24"
        Stack.Staging -> "10.12.30.0/24"
        Stack.Prod -> "10.16.30.0/24"
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

suspend fun privateSubnet2(env: Stack, vpc: Vpc) = subnet("${env.name.lowercase()}_private_subnet2") {
    args {
        vpcId(vpc.id)
        cidrBlock(privateCidr2(env))
        mapPublicIpOnLaunch(false)
        tags(envTags(env, "private-subnet2"))
    }
}

suspend fun privateSubnetGroup(env: Stack, subnet1: Subnet, subnet2: Subnet): SubnetGroup {
    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })
    return subnetGroup("${env.name.lowercase()}-mongo-subnet-group") {
        args {
            description("Custom subnet group for DocumentDB cluster")
            subnetIds(subnet1Id, subnet2Id)
        }
    }
}

suspend fun natSecurityGroup(env: Stack) = securityGroup("${env.name.lowercase()}_nat_server_sg") {
    args {
        tags(envTags(env, "nat-security-group"))
    }
}
