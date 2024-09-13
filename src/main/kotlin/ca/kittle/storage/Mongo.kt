package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.docdb.kotlin.Cluster
import com.pulumi.aws.docdb.kotlin.SubnetGroup
import com.pulumi.aws.docdb.kotlin.cluster
import com.pulumi.aws.docdb.kotlin.clusterInstance
import com.pulumi.aws.ec2.kotlin.SecurityGroup

suspend fun mongoCluster(
    env: Stack,
    securityGroup: SecurityGroup,
    subnetGroup: SubnetGroup
): Cluster {
    val securityGroupId = securityGroup.id.applyValue(fun(name: String): String { return name })
    val subnetGroupName = subnetGroup.name.applyValue(fun(name: String): String { return name })
    val password = System.getenv("MONGODB_PASSWORD") ?: error { "Mongo password not set" }
    return cluster("${env.name.lowercase()}-dmseer-mongo") {
        args {
            backupRetentionPeriod(7)
            clusterIdentifier("${env.name.lowercase()}-dmseer-mongo-cluster")
            dbSubnetGroupName(subnetGroupName)
            engine("docdb")
            masterUsername("qndadmin")
            masterPassword(password)
            availabilityZones("ca-central-1a", "ca-central-1b")
            preferredBackupWindow("04:00-07:00")
            storageEncrypted(env.name == "Prod")
            vpcSecurityGroupIds(listOf(securityGroupId))
            skipFinalSnapshot(true)
            tags(envTags(env, "${env.name.lowercase()}-docdb"))
        }
    }
}

suspend fun mongoInstances(env: Stack, cluster: Cluster) {
    val mongoClusterName = cluster.clusterIdentifier.applyValue(fun(name: String): String { return name })
    val nodes = if (env == Stack.Prod) 4 else 2
    for (i in 1..nodes) {
        clusterInstance("${env.name.lowercase()}-cluster-instance-$i") {
            args {
                identifier("${env.name.lowercase()}-mongo-cluster-instance-$i")
                clusterIdentifier(mongoClusterName)
                instanceClass("db.t3.medium")
                tags(envTags(env, "${env.name.lowercase()}-cluster-instance-$i"))
            }
        }
    }
}