package ca.kittle.network

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.acm.kotlin.Certificate
import com.pulumi.aws.alb.Listener
import com.pulumi.aws.alb.ListenerArgs
import com.pulumi.aws.alb.inputs.ListenerDefaultActionArgs
import com.pulumi.aws.alb.kotlin.LoadBalancer
import com.pulumi.aws.alb.kotlin.loadBalancer
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ec2.kotlin.Subnet
import com.pulumi.aws.lb.kotlin.TargetGroup
import com.pulumi.core.Output


suspend fun createLoadbalancer(env: Stack, subnet1Id: Output<String>, subnet2Id: Output<String>, elbSG: SecurityGroup): LoadBalancer {
    val sgId = elbSG.id.applyValue(fun(name: String): String { return name })

    return loadBalancer("${env.name.lowercase()}-dmseer-loadbalancer") {
        args {
            loadBalancerType("application")
            securityGroups(sgId)
            subnets(subnet1Id, subnet2Id)
            tags(envTags(env, "${env.name.lowercase()}-dmseer-loadbalancer"))
        }
    }
}

suspend fun createListeners(
    env: Stack,
    loadBalancer: LoadBalancer,
    targetGroup: TargetGroup,
    certificate: Certificate
): Listener {

    val loadBalancerArn = loadBalancer.arn.applyValue(fun(arn: String): String { return arn })
    val certificateArn = certificate.arn.applyValue(fun(arn: String): String { return arn })
    val targetGroupArn = targetGroup.arn.applyValue(fun(arn: String): String { return arn })
    val http = Listener(
        "${env.name.lowercase()}-dmseer-http-listener", ListenerArgs.builder()
            .loadBalancerArn(loadBalancerArn)
            .port(80)
            .protocol("HTTP")
            .defaultActions(
                listOf(
                    ListenerDefaultActionArgs.builder()
                        .type("forward")
                        .targetGroupArn(targetGroupArn)
                        .build()
                )
            )
            .build())
    return Listener(
        "${env.name.lowercase()}-dmseer-https-listener", ListenerArgs.builder()
            .loadBalancerArn(loadBalancerArn)
            .port(443)
            .protocol("HTTPS")
            .sslPolicy("ELBSecurityPolicy-TLS13-1-2-2021-06")
//            .certificateArn(certificateArn)
            .certificateArn("arn:aws:acm:ca-central-1:814245790557:certificate/fa959296-b8a7-4465-b1d5-309ca005ee0a")
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

