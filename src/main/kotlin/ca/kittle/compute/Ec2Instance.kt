package ca.kittle.compute

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ec2.kotlin.Instance
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ec2.kotlin.enums.InstanceType
import com.pulumi.aws.ec2.kotlin.instance
import com.pulumi.aws.ssm.kotlin.SsmFunctions.getParameter
import com.pulumi.core.Output

private const val ECS_AL2023_X86_64_AMI_PARAMETER =
    "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
private const val ECS_AL2023_ARM64_AMI_PARAMETER =
    "/aws/service/ecs/optimized-ami/amazon-linux-2023/arm64/recommended/image_id"
private const val AL2023_X86_64_AMI_PARAMETER =
    "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
private const val AL2023_ARM64_AMI_PARAMETER =
    "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
private val ARM64_INSTANCE_PREFIXES = listOf(
    "a1.",
    "c6g.", "c6gd.", "c6gn.",
    "c7g.", "c7gd.", "c7gn.",
    "d4g.",
    "g5g.",
    "i4g.", "im4gn.", "is4gen.",
    "m6g.", "m6gd.",
    "m7g.", "m7gd.",
    "r6g.", "r6gd.",
    "r7g.", "r7gd.",
    "t4g.",
    "x2gd."
)

suspend fun createEc2Instance(
    stack: Stack,
    resourceName: String,
    subnetId: Output<String>,
    securityGroups: List<SecurityGroup>,
    instanceType: InstanceType,
    useDockerAmi: Boolean = true,
    associatePublicIpAddress: Boolean = false,
    iamInstanceProfileName: Output<String>? = null,
    userData: Output<String>? = null,
): Instance {
    val amiId = resolveAmiId(instanceType, useDockerAmi)
    val securityGroupIds = securityGroups.map { securityGroup ->
        securityGroup.id.applyValue(fun(id: String): String { return id })
    }

    return instance("${stack.stackName}-$resourceName") {
        args {
            ami(amiId)
            instanceType(instanceType)
            subnetId(subnetId)
            associatePublicIpAddress(associatePublicIpAddress)
            if (iamInstanceProfileName != null) {
                iamInstanceProfile(iamInstanceProfileName)
            }
            if (userData != null) {
                userData(userData)
            }
            vpcSecurityGroupIds(securityGroupIds)
            tags(envTags(stack, resourceName))
        }
    }
}

private suspend fun resolveAmiId(instanceType: InstanceType, useDockerAmi: Boolean): String {
    val instanceTypeValue = instanceType.toJava().value
    val isArm64 = ARM64_INSTANCE_PREFIXES.any { prefix -> instanceTypeValue.startsWith(prefix) }
    val parameterName = when {
        useDockerAmi && isArm64 -> ECS_AL2023_ARM64_AMI_PARAMETER
        useDockerAmi -> ECS_AL2023_X86_64_AMI_PARAMETER
        isArm64 -> AL2023_ARM64_AMI_PARAMETER
        else -> AL2023_X86_64_AMI_PARAMETER
    }

    return getParameter(parameterName, false).value
}
