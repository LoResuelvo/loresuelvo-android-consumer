package com.loresuelvo.consumer.ui.auth

/**
 * Tag interface so the BDD step defs can resolve the production
 * `AuthSessionStore` without importing the full
 * `domain.auth.AuthSessionStore`. Removed after the fake-backed BDD
 * pass; the production binding comes from `di/RepositoryModule`.
 */
interface CompleteProfileSessionStoreStub
