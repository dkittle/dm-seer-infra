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
import com.pulumi.core.Output


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

suspend fun createVpc(env: Stack) = vpc("${env.stackName}-vpc") {
    args {
        cidrBlock(vpcCidr(env))
        enableDnsHostnames(true)
        enableDnsSupport(true)
        tags(envTags(env, "${env.stackName}-vpc"))
    }
}

suspend fun publicSubnet(env: Stack, vpc: Vpc) = subnet("${env.stackName}-public-subnet") {
    args {
        vpcId(vpc.id)
        cidrBlock(publicCidr(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1a")
        tags(envTags(env, "${env.stackName}-public-subnet"))
    }
}

suspend fun publicSubnet2(env: Stack, vpc: Vpc) = subnet("${env.stackName}-public-subnet2") {
    args {
        vpcId(vpc.id)
        cidrBlock(publicCidr2(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1b")
        tags(envTags(env, "${env.stackName}-public-subnet2"))
    }
}

suspend fun privateSubnet(env: Stack, vpc: Vpc) = subnet("${env.stackName}-private-subnet") {
    args {
        vpcId(vpc.id)
        cidrBlock(privateCidr(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1a")
        tags(envTags(env, "${env.stackName}-private-subnet"))
    }
}

suspend fun privateSubnet2(env: Stack, vpc: Vpc) = subnet("${env.stackName}-private-subnet2") {
    args {
        vpcId(vpc.id)
        cidrBlock(privateCidr2(env))
        mapPublicIpOnLaunch(false)
        availabilityZone("ca-central-1b")
        tags(envTags(env, "${env.stackName}-private-subnet2"))
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
    return subnetGroup("${env.stackName}-private-subnet-group") {
        args {
            description("Private subnet group")
            subnetIds(subnet1Id, subnet2Id)
        }
    }
}

suspend fun createFargateTargetGroup(env: Stack, vpcId: Output<String>): TargetGroup {
    return targetGroup("${env.stackName}-dmseer-target-group") {
        args {
            name("${env.stackName}-dmseer-target-group")
            port(8081)
            protocol("HTTP")
            targetType("ip")
            vpcId(vpcId)
            tags(envTags(env, "${env.stackName}-dmseer-target-group"))
        }
    }
}


suspend fun createInternetGateway(env: Stack, vpc: Vpc): InternetGateway {
    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    return internetGateway("${env.stackName}-internet-gateway") {
        args {
            vpcId(vpcId)
        }
    }
}

suspend fun updateRouteTable(env: Stack, vpc: Vpc, igw: InternetGateway) {
    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    val routeTableId = vpc.defaultRouteTableId.applyValue(fun(name: String): String { return name })
    val igwId = igw.id.applyValue(fun(name: String): String { return name })
    route("${env.stackName}-route-igw") {
        args {
            routeTableId(routeTableId)
            destinationCidrBlock("0.0.0.0/0")
            gatewayId(igwId)
        }
    }
}

suspend fun updateRouteTableOld(env: Stack, vpc: Vpc, igw: InternetGateway): RouteTable {
    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
    val routeTableId = vpc.mainRouteTableId.applyValue(fun(name: String): String { return name })
    val igwId = igw.id.applyValue(fun(name: String): String { return name })
    return RouteTable(
        "${env.stackName}-route-table", RouteTableArgs.builder()
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
            ).tags(mapOf("Name" to "${env.stackName}-route-table")).build()
    )
}

suspend fun createInterfaceEndpoints(env: Stack, vpcId: Output<String>, routeTableId: Output<String>) {
//    val vpcId = vpc.id.applyValue(fun(name: String): String { return name })
//    val rtId = vpc.defaultRouteTableId.applyValue(fun(name: String): String { return name })
    vpcEndpoint("${env.stackName}-dmseer-ecsagent-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecs-agent")
            vpcEndpointType("Interface")
        }
    }
    vpcEndpoint("${env.stackName}-dmseer-ecstelemetry-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecs-telemetry")
            vpcEndpointType("Interface")
        }
    }

    vpcEndpoint("${env.stackName}-dmseer-ecr-dkr-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecr.dkr")
            vpcEndpointType("Interface")
        }
    }

    vpcEndpoint("${env.stackName}-dmseer-ecr-api-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecr.api")
            vpcEndpointType("Interface")
        }
    }

    vpcEndpoint("${env.stackName}-dmseer-ecs-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.ecs")
            vpcEndpointType("Interface")
        }
    }

    val sg = securityGroup("${env.stackName}-s3-endpoint-securitygroup") {
        args {
            vpcId(vpcId)
            ingress {
                protocol("tcp")
                fromPort(443) // HTTPS port for S3 endpoint
                toPort(443)
                cidrBlocks("0.0.0.0/0")
                description("Allow HTTPS traffic to S3")
            }
            egress {
                protocol("-1")
                fromPort(0)
                toPort(0)
                cidrBlocks("0.0.0.0/0")
                description("Allow any egress")
            }
        }
    }
    val sgId = sg.id.applyValue(fun(name: String): String { return name })

    vpcEndpoint("${env.stackName}-dmseer-s3-endpoint") {
        args {
            vpcId(vpcId)
            serviceName("com.amazonaws.ca-central-1.s3")
            vpcEndpointType("Gateway")
            routeTableIds(routeTableId)
//            securityGroupIds(sgId)
        }
    }

}

