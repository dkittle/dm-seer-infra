package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.docdb.kotlin.Cluster
import com.pulumi.aws.docdb.kotlin.SubnetGroup
import com.pulumi.aws.docdb.kotlin.cluster
import com.pulumi.aws.docdb.kotlin.clusterInstance
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.ssm.kotlin.SsmFunctions.getParameter
import com.pulumi.resources.StackReference

suspend fun mongoCluster(
    env: Stack,
    securityGroup: SecurityGroup,
    subnetGroup: SubnetGroup,
    configStack: StackReference
): Cluster {
    val securityGroupId = securityGroup.id.applyValue(fun(name: String): String { return name })
    val subnetGroupName = subnetGroup.name.applyValue(fun(name: String): String { return name })
    val mongoUsername = getParameter("/shared-config/${env.stackName}/mongodb/username", false)
    val mongoPassword = getParameter("/shared-config/${env.stackName}/mongodb/password", true)
    return cluster("${env.stackName}-dmseer-mongo") {
        args {
            backupRetentionPeriod(7)
            clusterIdentifier("${env.name.lowercase()}-dmseer-mongo-cluster-34654da")
            dbSubnetGroupName(subnetGroupName)
            engine("docdb")
            masterUsername(mongoUsername.value)
            masterPassword(mongoPassword.value)
            availabilityZones("ca-central-1a", "ca-central-1b")
            preferredBackupWindow("04:00-07:00")
            storageEncrypted(env.name == "Prod")
            vpcSecurityGroupIds(listOf(securityGroupId))
            skipFinalSnapshot(true)
            tags(envTags(env, "${env.stackName}-docdb"))
        }
    }
}

suspend fun mongoInstances(env: Stack, cluster: Cluster) {
    val mongoClusterIdentifier = cluster.clusterIdentifier.applyValue(fun(name: String): String { return name })
//    val nodes = if (env == Stack.Prod) 4 else 2
    val nodes = 2
    for (i in 1..nodes) {
        clusterInstance("${env.name.lowercase()}-cluster-instance-$i") {
            args {
                identifier("${env.name.lowercase()}-mongo-cluster-instance-$i-687ad769")
                clusterIdentifier(mongoClusterIdentifier)
                instanceClass("db.t3.medium")
                tags(envTags(env, "${env.name.lowercase()}-cluster-instance-$i"))
            }
        }
    }
}