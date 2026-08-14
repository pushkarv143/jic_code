package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Transport: drivers, buses, routes, pickup points, student assignments.     */
/* ------------------------------------------------------------------------- */

@Serializable
data class DriverDto(
    val id: Long,
    val name: String = "",
    val phone: String? = null,
    val licenseNumber: String? = null,
    val address: String? = null,
)

@Serializable
data class DriverRequestDto(
    val name: String,
    val phone: String,
    val licenseNumber: String,
    val address: String? = null,
)

@Serializable
data class BusDto(
    val id: Long,
    val busNumber: String = "",
    val capacity: Int = 0,
    val driverId: Long? = null,
    val driverName: String? = null,
    val vehicleModel: String? = null,
    val registrationNumber: String? = null,
    val routeCount: Int? = null,
)

@Serializable
data class BusRequestDto(
    val busNumber: String,
    val capacity: Int,
    val driverId: Long? = null,
    val vehicleModel: String? = null,
    val registrationNumber: String,
)

@Serializable
data class RouteDto(
    val id: Long,
    val routeName: String = "",
    val busId: Long = 0,
    val busNumber: String? = null,
    val startPoint: String? = null,
    val endPoint: String? = null,
    val pickupPointCount: Int? = null,
)

@Serializable
data class RouteRequestDto(
    val routeName: String,
    val busId: Long,
    val startPoint: String,
    val endPoint: String,
)

@Serializable
data class PickupPointDto(
    val id: Long,
    val routeId: Long = 0,
    val pointName: String = "",
    val pickupTime: String? = null,
    val dropTime: String? = null,
)

@Serializable
data class PickupPointRequestDto(
    val pointName: String,
    val pickupTime: String,
    val dropTime: String,
)

@Serializable
data class StudentTransportDto(
    val id: Long,
    val studentId: Long = 0,
    val studentName: String? = null,
    val admissionNumber: String? = null,
    val routeId: Long = 0,
    val routeName: String? = null,
    val pickupPointId: Long = 0,
    val pickupPointName: String? = null,
    val monthlyFee: Double = 0.0,
)

@Serializable
data class StudentTransportRequestDto(
    val studentId: Long,
    val routeId: Long,
    val pickupPointId: Long,
    val monthlyFee: Double,
)
