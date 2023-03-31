package ca.kittle

import com.pulumi.aws.ec2.kotlin.Vpc
import com.pulumi.aws.ec2.kotlin.subnetResource
import com.pulumi.aws.ec2.kotlin.vpcResource

private const val VPC_CIDR = "10.10.0.0/16"

suspend fun vpc(env: String) = vpcResource("${env}_vpc") {
    args {
        cidrBlock(VPC_CIDR)
        enableDnsHostnames(true)
        enableDnsSupport(true)
        tags(
            "Name" to "$env-vpc",
            "env" to env
        )
    }
}

suspend fun publicSubnet(env: String, vpc: Vpc) = subnetResource("${env}_public_subnet") {
    args {
        vpcId(vpc.id)
        cidrBlock("10.10.1.0/24")
        mapPublicIpOnLaunch(true)
        tags(
            "Name" to "$env-public-subnet",
            "env" to env
        )
    }
}
