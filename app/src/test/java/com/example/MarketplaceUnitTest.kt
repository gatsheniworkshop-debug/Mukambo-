package com.example

import com.example.data.model.MarketplaceConstants
import com.example.data.model.PaymentGatewayType
import com.example.data.model.ZimbabweLocations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceUnitTest {

    @Test
    fun distanceBetweenHarareAndBulawayo_isRealistic() {
        val harare = ZimbabweLocations.findCity("Harare")
        val bulawayo = ZimbabweLocations.findCity("Bulawayo")
        val km = ZimbabweLocations.distanceKm(
            harare.latitude, harare.longitude,
            bulawayo.latitude, bulawayo.longitude
        )
        assertTrue("Expected Harare-Bulawayo ~360km, got $km", km in 330.0..410.0)
    }

    @Test
    fun ecoCashAndOneMoneyValidation_enforcesZimbabwePrefixesAndMasks() {
        val ecoValid = PaymentGatewayType.ECOCASH.validateAndMask("0771234567")
        assertTrue(ecoValid.isSuccess)
        assertEquals("+26377****567", ecoValid.getOrThrow())

        val ecoWrongPrefix = PaymentGatewayType.ECOCASH.validateAndMask("0711234567")
        assertTrue(ecoWrongPrefix.isFailure)

        val oneValid = PaymentGatewayType.ONEMONEY.validateAndMask("+263719876543")
        assertTrue(oneValid.isSuccess)
        assertEquals("+26371****543", oneValid.getOrThrow())
    }

    @Test
    fun zimSwitchCardValidation_usesLuhnAndMasksLast4() {
        val validCard = PaymentGatewayType.ZIMSWITCH_CARD.validateAndMask(
            rawAccount = "4111 1111 1111 1111",
            expiry = "09/28",
            cvv = "321"
        )
        assertTrue(validCard.isSuccess)
        assertEquals("Card •••• 1111", validCard.getOrThrow())

        val invalidLuhn = PaymentGatewayType.ZIMSWITCH_CARD.validateAndMask(
            rawAccount = "4111 1111 1111 1112",
            expiry = "09/28",
            cvv = "321"
        )
        assertTrue(invalidLuhn.isFailure)
    }

    @Test
    fun currencyFormatting_formatsUsdAndZig() {
        assertEquals("$420", MarketplaceConstants.formatPrice(420.0, "USD"))
        assertEquals("ZiG 2,750", MarketplaceConstants.formatPrice(100.0, "ZIG"))
    }

    @Test
    fun allTenZimbabweProvinces_haveMappedCities() {
        assertEquals(10, ZimbabweLocations.provinces.size)
        ZimbabweLocations.provinces.forEach { province ->
            val cities = ZimbabweLocations.citiesInProvince(province)
            assertTrue("Expected at least one city in $province", cities.isNotEmpty())
        }
    }
}
