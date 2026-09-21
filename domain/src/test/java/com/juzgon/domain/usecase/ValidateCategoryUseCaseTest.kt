package com.juzgon.domain.usecase

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidateCategoryUseCaseTest {
    private val useCase = ValidateCategoryUseCase()

    @Test
    fun `empty category name fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(Category(name = " ", attributes = listOf(Attribute("quality"))))
        }
    }

    @Test
    fun `no attributes fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(Category(name = "Restaurant", attributes = emptyList()))
        }
    }

    @Test
    fun `duplicate attribute names fail case insensitive`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                Category(
                    name = "Restaurant",
                    attributes = listOf(Attribute("Quality"), Attribute("quality")),
                ),
            )
        }
    }

    @Test
    fun `valid category passes`() {
        useCase(Category(name = "Restaurant", attributes = listOf(Attribute("Quality"), Attribute("Value"))))
    }

    @Test
    fun `duplicate diamond chart order fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                Category(
                    name = "Cars",
                    attributes =
                        listOf(
                            Attribute("Speed", diamondOrder = 1),
                            Attribute("Brakes", diamondOrder = 1),
                        ),
                ),
            )
        }
    }

    @Test
    fun `non numeric diamond chart attribute fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                Category(
                    name = "Cars",
                    attributes =
                        listOf(
                            Attribute(
                                id = "Photo",
                                type = AttributeType.IMAGE,
                                displayInDiamond = true,
                            ),
                        ),
                ),
            )
        }
    }
}
