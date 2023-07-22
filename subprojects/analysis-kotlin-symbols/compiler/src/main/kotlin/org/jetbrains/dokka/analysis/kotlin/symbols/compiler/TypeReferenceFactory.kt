package org.jetbrains.dokka.analysis.kotlin.symbols.compiler

import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.types.*

internal fun KtAnalysisSession.getTypeReferenceFrom(type: KtType): TypeReference =
    getTypeReferenceFromPossiblyRecursive(type, emptyList())


// see `deep recursive typebound #1342` test
private fun KtAnalysisSession.getTypeReferenceFromPossiblyRecursive(type: KtType, paramTrace: List<KtType>): TypeReference {
    if(type is KtTypeParameterType) {
        // compare by symbol since, e.g. T? and T have the different KtType, but the same type parameter
        paramTrace.indexOfFirst { it is KtTypeParameterType && type.symbol == it.symbol }
            .takeIf { it >= 0 }
            ?.let{ return RecursiveType(it) }
    }

    return when (type) {
        is KtNonErrorClassType -> TypeConstructor(
            type.classId.asFqNameString(), // TODO: remove '!!'
            type.ownTypeArguments.map {
                getTypeReferenceFromTypeProjection(
                    it,
                    paramTrace
                )
            }
        )
        is KtTypeParameterType -> {
            val upperBoundsOrNullableAny = type.symbol.upperBounds.takeIf { it.isNotEmpty() } ?: listOf(this.builtinTypes.NULLABLE_ANY)

            TypeParam(bounds = upperBoundsOrNullableAny.map {
                getTypeReferenceFromPossiblyRecursive(
                    it,
                    paramTrace + type
                )
            })
        }
        is KtClassErrorType ->  TypeConstructor(type.errorMessage, emptyList())
        is KtFlexibleType ->  getTypeReferenceFromPossiblyRecursive(
            type.upperBound,
            paramTrace
        )
        is KtDefinitelyNotNullType ->  getTypeReferenceFromPossiblyRecursive(
            type.original,
            paramTrace
        )
        is KtCapturedType -> TODO()
        is KtDynamicType -> TypeConstructor("[dynamic]", emptyList())
        is KtIntegerLiteralType -> TODO()
        is KtIntersectionType -> TODO()
        is KtTypeErrorType -> TODO()
    }.let {
        if (type.isMarkedNullable) org.jetbrains.dokka.links.Nullable(it) else it
    }

}

// TODO

private fun KtAnalysisSession.getTypeReferenceFromTypeProjection(typeProjection: KtTypeProjection, paramTrace: List<KtType>): TypeReference =
    when (typeProjection) {
        is KtStarTypeProjection -> StarProjection
        is KtTypeArgumentWithVariance -> getTypeReferenceFromPossiblyRecursive(typeProjection.type, paramTrace)
    }
