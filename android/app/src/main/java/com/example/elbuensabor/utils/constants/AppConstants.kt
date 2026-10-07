package com.example.elbuensabor.utils.constants

/**
 * Constantes generales de la aplicación.
 *
 * El correo ADMIN_EMAIL identifica al administrador principal y sirve para
 * arrancar la administración por primera vez. Después, cualquier usuario cuyo
 * documento en Firestore tenga rol = ADMIN también puede usar el panel de
 * administración y crear/promover otros administradores.
 */
object AppConstants {
    const val ROLE_CLIENT = "CLIENTE"
    const val ROLE_ADMIN = "ADMIN"

    const val ADMIN_EMAIL = "admin@elbuensabor.com"
}
