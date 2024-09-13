package ca.kittle.network

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.docdb.kotlin.SubnetGroup
import com.pulumi.aws.docdb.kotlin.subnetGroup
import com.pulumi.aws.ec2.RouteTable
import com.pulumi.aws.ec2.RouteTableArgs
import com.pulumi.aws.ec2.inputs.RouteTableRouteArgs
import com.pulumi.aws.ec2.kotlin.*
import com.pulumi.aws.lb.kotlin.TargetGroup
import com.pulumi.aws.lb.kotlin.targetGroup


fun vpcCidr(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.0.0/16"
        Stack.Staging -> "10.14.0.0/16"
        Stack.Prod -> "10.16.0.0/16"
    }

private fun publicCidr(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.10.0/24"
        Stack.Staging -> "10.14.10.0/24"
        Stack.Prod -> "10.16.10.0/24"
    }

private fun privateCidr(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.20.0/24"
        Stack.Staging -> "10.14.20.0/24"
        Stack.Prod -> "10.16.20.0/24"
    }

private fun publicCidr2(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.30.0/24"
        Stack.Staging -> "10.14.30.0/24"
        Stack.Prod -> "10.16.30.0/24"
    }

private fun privateCidr2(env: Stack): String =
    when (env) {
        Stack.Dev -> "10.10.40.0/24"
        Stack.Staging -> "10.14.40.0/24"
        Stack.Prod -> "10.16.40.0/24"
    }

suspend fun createVpc(env: Stack) = vpc("${env.stackEnv}-vpc") {
    args {
        cidrBlock(vpcCidr(env))
        enableDnsHostnames(true)
        enableDnsSupport(true)
        tags(envTags(env, "${env.stackEnv}-vpc"))
    }
}

suspend fun publicSubnet(env: Stack, vpc: Vpc) = subnet("${env.stackEnv}-public-subnet") {
    args {
        vpcId(vpc.id)
        cidrBlock(publicCidr(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1a")
        tags(envTags(env, "${env.stackEnv}-public-subnet"))
    }
}

suspend fun publicSubnet2(env: Stack, vpc: Vpc) = subnet("${env.stackEnv}-public-subnet2") {
    args {
        vpcId(vpc.id)
        cidrBlock(publicCidr2(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1b")
        tags(envTags(env, "${env.stackEnv}-public-subnet2"))
    }
}

suspend fun privateSubnet(env: Stack, vpc: Vpc) = subnet("${env.stackEnv}-private-subnet") {
    args {
        vpcId(vpc.id)
        cidrBlock(privateCidr(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1a")
        tags(envTags(env, "${env.stackEnv}-private-subnet"))
    }
}

suspend fun privateSubnet2(env: Stack, vpc: Vpc) = subnet("${env.stackEnv}-private-subnet2") {
    args {
        vpcId(vpc.id)
        cidrBlock(privateCidr2(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1b")
        tags(envTags(env, "${env.stackEnv}-private-subnet2"))
    }
}

suspend fun publicSubnetGroup(env: Stack, subnet1: Subnet, subnet2: Subnet): SubnetGroup {
    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })
    return subnetGroup("${env.name.lowercase()}-public-subnet-group") {
        args {
            description("Public subnet group")
            subnetIds(subnet1Id, subnet2Id)
        }
    }
}

suspend fun privateSubnetGroup(env: Stack, subnet1: Subnet, subnet2: Subnet): SubnetGroup {
    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })
    return subnetGroup("${env.name.lowercase()}-private-subnet-group") {
        args {
            description("Private subnet group")
            subnetIds(subnet1Id, subnet2Id)
        }
    }
}

suspend fun createTargetGroup(env: Stack, vpc: Vpc): TargetGroup = targetGroup("${env.name.lowercase()}-dmseer-target-group") {
    args {
        name("${env.stackEnv}-dmseer-target-group")
        port(80)
        protocol("HTTP")
        targetType("instance")
        vpcId(vpc.id)
        tags(envTags(env, "${env.name.lowercase()}-dmseer-target-group"))
    }
}

suspend fun createInternetGateway(env: Stack, vpc: Vpc): InternetGateway {
    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    return internetGateway("${env.name.lowercase()}-internet-gateway") {
        args {
            vpcId(vpcId)
        }
    }
}

suspend fun updateRouteTable(env: Stack, vpc: Vpc, igw: InternetGateway) {
    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    val igwId = igw.id.applyValue(fun(name: String): String { return name })
    val rt = RouteTable(
        "${env.stackEnv}-route-table", RouteTableArgs.builder()
            .vpcId(vpcId)
            .routes(
                RouteTableRouteArgs.builder()
                    .cidrBlock("0.0.0.0/0")
                    .gatewayId(igwId)
                    .build(),
                RouteTableRouteArgs.builder()
                    .cidrBlock(vpcCidr(env))
                    .gatewayId("local")
                    .build()
        ).tags(mapOf("Name" to "${env.stackEnv}-route-table")).build()
    )
}


suspend fun createInterfaceEndpoints(env: Stack, vpc: Vpc) {
    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    vpcEndpoint("${env.stackEnv}-dmseer-ecsagent-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecs-agent")
            vpcEndpointType("Interface")
        }
    }
    vpcEndpoint("${env.stackEnv}-dmseer-ecstelemetry-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecs-telemetry")
            vpcEndpointType("Interface")
        }
    }

    vpcEndpoint("${env.stackEnv}-dmseer-ecs-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecs")
            vpcEndpointType("Interface")
        }
    }

}

