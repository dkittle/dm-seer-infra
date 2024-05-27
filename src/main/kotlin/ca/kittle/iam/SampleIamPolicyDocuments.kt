package ca.kittle.iam

import com.pulumi.aws.iam.inputs.GetPolicyDocumentArgs
import com.pulumi.aws.iam.inputs.GetPolicyDocumentStatementArgs
import com.pulumi.aws.iam.inputs.GetPolicyDocumentStatementPrincipalArgs
import com.pulumi.aws.iam.kotlin.IamFunctions
import com.pulumi.core.Output

//private val assumeRole = IamFunctions.getPolicyDocument(
//    GetPolicyDocumentArgs.builder()
//        .statements(
//            GetPolicyDocumentStatementArgs.builder()
//                .effect("Allow")
//                .actions("sts:AssumeRole")
//                .principals(
//                    GetPolicyDocumentStatementPrincipalArgs.builder()
//                        .type("Service")
//                        .identifiers("glue.amazonaws.com")
//                        .build()
//                ).build()
//        ).build()
//)
//
//
//private val policy = IamFunctions.getPolicyDocument(
//    GetPolicyDocumentArgs.builder()
//        .statements(
//            GetPolicyDocumentStatementArgs.builder()
//                .effect("Allow")
//                .actions("s3:GetObject*", "s3:ListObject*")
//                .resources(makeBucketArns(dataCatalogInput.sourceBucket.arn()))
//                .build()
//        ).build()
//)

fun makeBucketArns(arn: Output<String>): Output<List<String>>? {
    return arn.applyValue { v: String ->
        listOf(v, "$v/*")
    }
}