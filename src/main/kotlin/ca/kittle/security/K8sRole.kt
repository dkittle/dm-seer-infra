package ca.kittle.security

import ca.kittle.Stack
import com.pulumi.aws.iam.kotlin.IamFunctions
import com.pulumi.aws.iam.kotlin.Role
import com.pulumi.aws.iam.kotlin.inputs.GetPolicyDocumentPlainArgs
import com.pulumi.aws.iam.kotlin.inputs.GetPolicyDocumentStatement
import com.pulumi.aws.iam.kotlin.inputs.GetPolicyDocumentStatementPrincipal
import com.pulumi.aws.iam.kotlin.role
import com.pulumi.kubernetes.core.v1.kotlin.ServiceAccount
import com.pulumi.kubernetes.core.v1.kotlin.serviceAccount

private val nodegroupManagedPolicyArns: List<String> = listOf(
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
)


//suspend fun createK8sServiceAccount(env: Stack): ServiceAccount {
//    return serviceAccount("${env.name.lowercase()}-k8n-service-account") {
//        args {
//            name("${env.name.lowercase()}-k8n-service-account")
//
//        }
//}

suspend fun createK8SRole(): Role {
    val assumeRole = IamFunctions.getPolicyDocument(
        GetPolicyDocumentPlainArgs(
            statements = listOf(
                GetPolicyDocumentStatement(
                    effect = "Allow",
                    principals = listOf(
                        GetPolicyDocumentStatementPrincipal(
                            type = "Service",
                            identifiers = listOf("eks.amazonaws.com")
                        )
                    ),
                    actions = listOf("sts:AssumeRole")
                )
            )
        )
    )

    val adminsRole = role("admins-role") {

    }
    val policyDocumentResult = assumeRole.json
    val k8sRole = role("eks-cluster-role") {
        args {
            name("dmseer-eks-cluster-role")
            assumeRolePolicy(policyDocumentResult)
        }
    }

    return k8sRole
}

//    val exampleAmazonEKSClusterPolicy = RolePolicyAttachment(
//        "example-AmazonEKSClusterPolicy", RolePolicyAttachmentArgs.builder()
//            .policyArn("arn:aws:iam::aws:policy/AmazonEKSClusterPolicy")
//            .role(example.name())
//            .build()
//    )
//    // Optionally, enable Security Groups for Pods
//    // Reference: https://docs.aws.amazon.com/eks/latest/userguide/security-groups-for-pods.html
//    val exampleAmazonEKSVPCResourceController = RolePolicyAttachment(
//        "example-AmazonEKSVPCResourceController", RolePolicyAttachmentArgs.builder()
//            .policyArn("arn:aws:iam::aws:policy/AmazonEKSVPCResourceController")
//            .role(example.name())
//            .build()
//    )
