package ca.kittle.network

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.acm.kotlin.Certificate
import com.pulumi.aws.alb.Listener
import com.pulumi.aws.alb.ListenerArgs
import com.pulumi.aws.alb.enums.LoadBalancerType
import com.pulumi.aws.alb.inputs.ListenerDefaultActionArgs
import com.pulumi.aws.alb.kotlin.LoadBalancer
import com.pulumi.aws.alb.kotlin.loadBalancer
import com.pulumi.aws.ec2.kotlin.Subnet
import com.pulumi.aws.lb.kotlin.TargetGroup


suspend fun createLoadbalancer(env: Stack, subnet1: Subnet, subnet2: Subnet): LoadBalancer {

    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })

    return loadBalancer("${env.name.lowercase()}-dmseer-loadbalancer") {
        args {
            loadBalancerType("application")
            subnets(subnet1Id, subnet2Id)
            tags(envTags(env, "${env.name.lowercase()}-dmseer-loadbalancer"))
        }
    }
}

suspend fun createListener(
    env: Stack,
    loadBalancer: LoadBalancer,
    targetGroup: TargetGroup,
    certificate: Certificate
): Listener {

    val loadBalancerArn = loadBalancer.arn.applyValue(fun(arn: String): String { return arn })
    val certificateArn = certificate.arn.applyValue(fun(arn: String): String { return arn })
    val targetGroupArn = targetGroup.arn.applyValue(fun(arn: String): String { return arn })

    return Listener(
        "${env.name.lowercase()}-dmseer-listener", ListenerArgs.builder()
            .loadBalancerArn(loadBalancerArn)
            .port(443)
            .protocol("HTTPS")
            .certificateArn(certificateArn)
            .defaultActions(
                listOf(
                    ListenerDefaultActionArgs.builder()
                        .type("forward")
                        .targetGroupArn(targetGroupArn)
                        .build()
                )
            )
            .build()
    )
}

