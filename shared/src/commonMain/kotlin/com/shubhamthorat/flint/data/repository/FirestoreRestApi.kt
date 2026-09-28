package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType as KtorContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreRestApi(
    private val projectId: String = "flint-f14f2",
    private val apiKey: String = "AIzaSyD8HK_j6JqduOivNgtAFgdf5i4sqpB5Cxs",
    private val httpClient: HttpClient = createDefaultHttpClient()
) {
    private val tag = "FirestoreRestApi"

    suspend fun saveCampaign(userId: String, campaign: Campaign, idToken: String? = null): FlintResult<Campaign, AppError> {
        val updateMask = "updateMask.fieldPaths=id&updateMask.fieldPaths=title&updateMask.fieldPaths=ideaOrSource&updateMask.fieldPaths=items&updateMask.fieldPaths=createdAtTimestamp"
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/users/$userId/campaigns/${campaign.id}?key=$apiKey&$updateMask"
        return try {
            val bodyJson = buildJsonObject {
                put("fields", buildJsonObject {
                    put("id", stringValue(campaign.id))
                    put("title", stringValue(campaign.title))
                    put("ideaOrSource", stringValue(campaign.ideaOrSource))
                    put("createdAtTimestamp", intValue(campaign.createdAtTimestamp))
                    put("items", buildJsonObject {
                        put("arrayValue", buildJsonObject {
                            put("values", buildJsonArray {
                                campaign.items.forEach { asset ->
                                    add(buildJsonObject {
                                        put("mapValue", buildJsonObject {
                                            put("fields", buildContentAssetFields(asset))
                                        })
                                    })
                                }
                            })
                        })
                    })
                })
            }

            val response = httpClient.patch(url) {
                contentType(KtorContentType.Application.Json)
                if (!idToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $idToken")
                }
                setBody(bodyJson.toString())
            }

            val responseText = response.bodyAsText()
            if (response.status.isSuccess()) {
                FlintLogger.i(tag, "Successfully saved Campaign ID ${campaign.id} to Firestore via REST")
                FlintResult.Success(campaign)
            } else if (response.status.value == 403) {
                FlintLogger.e(tag, "Firestore Permission Denied (HTTP 403). Security rules blocking writes.")
                FlintResult.Error(AppError.Storage("Firestore Security Rules Error (403): Access denied by Firebase Console Firestore Rules. Update Rules in Firebase Console to 'allow read, write: if true;'"))
            } else {
                FlintLogger.e(tag, "Failed to save Campaign via REST HTTP ${response.status.value}: $responseText")
                FlintResult.Error(AppError.Storage("Firestore REST error (${response.status.value}): $responseText"))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Exception saving Campaign via REST: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore REST error: ${e.message}"))
        }
    }

    suspend fun fetchCampaigns(userId: String, idToken: String? = null): List<Campaign> {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/users/$userId/campaigns?key=$apiKey"
        return try {
            val response = httpClient.get(url) {
                if (!idToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $idToken")
                }
            }
            if (!response.status.isSuccess()) return emptyList()
            val text = response.bodyAsText()
            val json = jsonParser.parseToJsonElement(text).jsonObject
            val documents = json["documents"]?.jsonArray ?: return emptyList()

            documents.mapNotNull { docElement ->
                parseCampaignFromDoc(docElement.jsonObject)
            }
        } catch (e: Exception) {
            FlintLogger.w(tag, "Failed to fetch campaigns via REST: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveContent(userId: String, asset: ContentAsset, idToken: String? = null): FlintResult<ContentAsset, AppError> {
        val updateMask = "updateMask.fieldPaths=id&updateMask.fieldPaths=sourceId&updateMask.fieldPaths=title&updateMask.fieldPaths=body&updateMask.fieldPaths=type&updateMask.fieldPaths=status&updateMask.fieldPaths=platform&updateMask.fieldPaths=createdAtTimestamp"
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/users/$userId/content/${asset.id}?key=$apiKey&$updateMask"
        return try {
            val bodyJson = buildJsonObject {
                put("fields", buildContentAssetFields(asset))
            }

            val response = httpClient.patch(url) {
                contentType(KtorContentType.Application.Json)
                if (!idToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $idToken")
                }
                setBody(bodyJson.toString())
            }

            val responseText = response.bodyAsText()
            if (response.status.isSuccess()) {
                FlintLogger.i(tag, "Successfully saved ContentAsset ID ${asset.id} (${asset.title}) to Firestore via REST")
                FlintResult.Success(asset)
            } else if (response.status.value == 403) {
                FlintLogger.e(tag, "Firestore Permission Denied (HTTP 403). Security rules blocking writes.")
                FlintResult.Error(AppError.Storage("Firestore Security Rules Error (403): Access denied by Firebase Console Firestore Rules. Update Rules in Firebase Console to 'allow read, write: if true;'"))
            } else {
                FlintLogger.e(tag, "Failed to save ContentAsset via REST HTTP ${response.status.value}: $responseText")
                FlintResult.Error(AppError.Storage("Firestore REST error (${response.status.value}): $responseText"))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Exception saving ContentAsset via REST: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore REST error: ${e.message}"))
        }
    }

    suspend fun fetchContentAssets(userId: String, idToken: String? = null): List<ContentAsset> {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/users/$userId/content?key=$apiKey"
        return try {
            val response = httpClient.get(url) {
                if (!idToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $idToken")
                }
            }
            if (!response.status.isSuccess()) return emptyList()
            val text = response.bodyAsText()
            val json = jsonParser.parseToJsonElement(text).jsonObject
            val documents = json["documents"]?.jsonArray ?: return emptyList()

            documents.mapNotNull { docElement ->
                val fields = docElement.jsonObject["fields"]?.jsonObject ?: return@mapNotNull null
                parseContentAssetFields(fields)
            }
        } catch (e: Exception) {
            FlintLogger.w(tag, "Failed to fetch ContentAssets via REST: ${e.message}")
            emptyList()
        }
    }

    private fun buildContentAssetFields(asset: ContentAsset): JsonObject {
        return buildJsonObject {
            put("id", stringValue(asset.id))
            put("sourceId", stringValue(asset.sourceId ?: ""))
            put("title", stringValue(asset.title))
            put("body", stringValue(asset.body))
            put("type", stringValue(asset.type.name))
            put("status", stringValue(asset.status.name))
            put("platform", stringValue(asset.platform))
            put("createdAtTimestamp", intValue(asset.createdAtTimestamp))
        }
    }

    private fun parseCampaignFromDoc(doc: JsonObject): Campaign? {
        val fields = doc["fields"]?.jsonObject ?: return null
        val id = getString(fields, "id") ?: return null
        val title = getString(fields, "title") ?: "Untitled Campaign"
        val ideaOrSource = getString(fields, "ideaOrSource") ?: ""
        val createdAtTimestamp = getLong(fields, "createdAtTimestamp") ?: 0L

        val itemsArray = fields["items"]?.jsonObject?.get("arrayValue")?.jsonObject?.get("values")?.jsonArray
        val items = itemsArray?.mapNotNull { itemElement ->
            val itemFields = itemElement.jsonObject["mapValue"]?.jsonObject?.get("fields")?.jsonObject
            if (itemFields != null) parseContentAssetFields(itemFields) else null
        } ?: emptyList()

        return Campaign(
            id = id,
            title = title,
            ideaOrSource = ideaOrSource,
            items = items,
            createdAtTimestamp = createdAtTimestamp
        )
    }

    private fun parseContentAssetFields(fields: JsonObject): ContentAsset? {
        val id = getString(fields, "id") ?: return null
        val sourceId = getString(fields, "sourceId")?.takeIf { it.isNotBlank() }
        val title = getString(fields, "title") ?: "Untitled Asset"
        val body = getString(fields, "body") ?: ""
        val typeStr = getString(fields, "type") ?: ContentType.LINKEDIN_POST.name
        val statusStr = getString(fields, "status") ?: ContentStatus.DRAFT.name
        val platform = getString(fields, "platform") ?: "Generic"
        val createdAtTimestamp = getLong(fields, "createdAtTimestamp") ?: 0L

        val type = try { ContentType.valueOf(typeStr) } catch (_: Exception) { ContentType.LINKEDIN_POST }
        val status = try { ContentStatus.valueOf(statusStr) } catch (_: Exception) { ContentStatus.DRAFT }

        return ContentAsset(
            id = id,
            sourceId = sourceId,
            title = title,
            body = body,
            type = type,
            status = status,
            platform = platform,
            createdAtTimestamp = createdAtTimestamp
        )
    }

    private fun stringValue(str: String): JsonObject = buildJsonObject { put("stringValue", JsonPrimitive(str)) }
    private fun intValue(value: Long): JsonObject = buildJsonObject { put("integerValue", JsonPrimitive(value.toString())) }

    private fun getString(fields: JsonObject, key: String): String? {
        return fields[key]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content
    }

    private fun getLong(fields: JsonObject, key: String): Long? {
        val valObj = fields[key]?.jsonObject ?: return null
        val intStr = valObj["integerValue"]?.jsonPrimitive?.content
        if (intStr != null) return intStr.toLongOrNull()
        val strVal = valObj["stringValue"]?.jsonPrimitive?.content
        return strVal?.toLongOrNull()
    }

    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(jsonParser)
                }
                install(io.ktor.client.plugins.HttpTimeout) {
                    requestTimeoutMillis = 30_000L
                    connectTimeoutMillis = 15_000L
                }
            }
        }
    }
}
