package ca.kittle.compute

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ec2.kotlin.Eip
import com.pulumi.aws.ec2.kotlin.Instance
import com.pulumi.aws.ec2.kotlin.eip

suspend fun createElasticIp(stack: Stack, resourceName: String, instance: Instance): Eip {
    val instanceId = instance.id.applyValue(fun(id: String): String { return id })
    return eip("${stack.stackName}-$resourceName") {
        args {
            domain("vpc")
            instance(instanceId)
            tags(envTags(stack, resourceName))
        }
    }
}
