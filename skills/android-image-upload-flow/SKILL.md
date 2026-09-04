# android-image-upload-flow

Load when adding or changing a flow that uploads files (image, audio, video) to a business resource (conversation message, job request, work order, profile photo).

## Do not load

- Pure UI changes that do not cross the data layer.
- Reading or rendering an already-uploaded file.
- One-off migrations: the same checklist applies, but reuse the existing `FilePurpose` value when possible.

## The four-step checklist

Every upload flow is the same shape. Skipping any step is a wire contract or layering bug.

1. **`FilePurpose`** (`domain/file/FilePurpose.kt`): add a new value if no existing one matches the business resource. Each purpose maps to a backend upload policy (mime / size / codec caps).
2. **`purposeToWire` / `purposeFromWire`** (`data/api/mapper/FileDtoMapper.kt`): add the matching wire constant. Both `when` branches are exhaustive — compile error if missed.
3. **Use case** (`domain/usecase/<area>/UploadXxxUseCase.kt`): orchestrate `presign → uploadBytes → confirm` per file via `FileRepository`. Return `Success(List<String>)` (confirmed file IDs) or typed `Failure.Network/Server/Unauthorized`.
4. **ViewModel**: invoke the use case before the parent resource call. Map failures into the same typed surface the parent uses (e.g., `ContactProviderError.Network`). Pass the returned IDs to the parent resource's data class.

## Why this shape

- `FilePurpose` is the only signal the backend uses to pick the upload policy. Reusing an existing purpose on a different business resource bypasses the policy and is a wire contract bug.
- The use case owns the pipeline so the ViewModel never touches `presign` / `uploadBytes` / `confirm` directly. Mirrors `UploadAttachmentsAndSendUseCase.kt:39-88` and `UploadJobRequestImagesUseCase.kt:54-66`.
- Returning IDs (not bytes) keeps the parent use case pure and trivially mockable.

## Failure mapping

Collapse the three step failures into one typed tree at the use case boundary. Short-circuit on the first failure — no partial uploads leak to the next file.

| Pipeline step           | Mapped to                                  |
|-------------------------|--------------------------------------------|
| `presign` Network       | `Outcome.Failure.Network(cause)`           |
| `presign` Server        | `Outcome.Failure.Server(code, "$msg for $name")` |
| `presign` Unauthorized  | `Outcome.Failure.Unauthorized(msg)`        |
| `uploadBytes` *         | same as `presign`                          |
| `confirm` *             | same as `presign`                          |

## ViewModel wiring

```kotlin
viewModelScope.launch {
    val uploaded = when (val o = uploadUseCase(state.attachments)) {
        is UploadOutcome.Success -> o.fileIds
        is UploadOutcome.Failure -> {
            _uiState.update { it.copy(error = o.toUiError()) }
            return@launch
        }
    }
    // Pass `uploaded` as `imageFileIds` / `audioFileId` to the parent resource.
}
```

The empty-input case short-circuits inside the use case to `Success(emptyList())`, so the ViewModel does not branch on `attachments.isEmpty()`.

## Validation

```bash
rg -n "import (com\.loresuelvo\.consumer\.(data|application|ui)|android\.|dagger|hilt|okhttp3|retrofit2|kotlinx\.serialization)" \
  app/src/main/java/com/loresuelvo/consumer/domain/usecase/<area>/
```

Expected: no matches. The use case depends on `domain/conversation/MediaUpload` and `domain/file/*` only.

## Reference implementations

- `domain/usecase/diagnosis/UploadAttachmentsAndSendUseCase.kt` (chat with AI)
- `domain/usecase/jobrequest/UploadJobRequestImagesUseCase.kt` (contact-provider flow)