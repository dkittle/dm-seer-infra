package ca.kittle.cluster

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ecs.kotlin.Cluster
import com.pulumi.aws.ecs.kotlin.cluster

//import com.pulumi.aws.eks.kotlin.Cluster
//import com.pulumi.aws.eks.kotlin.cluster
//import com.pulumi.aws.iam.kotlin.Role


suspend fun createCluster(env: Stack): Cluster {
    return cluster("${env.stackName}-dmseer-ecs-cluster") {
        args {
            name("${env.stackName}-dmseer-ecs-cluster")
            tags(envTags(env, "${env.name.lowercase()}-dmseer-ecs-cluster"))
        }
    }
}

//suspend fun cluster(env: Stack, role: Role, subnet1: Subnet, subnet2: Subnet): Cluster {
//    val subnet1Id = subnet1.id.applyValue(fun(name: String): String { return name })
//    val subnet2Id = subnet2.id.applyValue(fun(name: String): String { return name })
//    val roleUrn = role.urn.applyValue(fun(urn: String): String { return urn })
//
//    return cluster("${env.name.lowercase()}-dmseer-cluster") {
//        args {
//            name("${env.name.lowercase()}-dmseer-cluster")
//            roleArn(roleUrn)
//            vpcConfig {
//                subnetIds(listOf(subnet1Id, subnet2Id))
//            }
//            tags(envTags(env, "kubernetes-cluster"))
//        }
//    }
//}

