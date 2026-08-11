package com.soloprono.motorsonar.util

import kotlin.math.roundToInt

object RepairCostEstimator {
    
    enum class PartsQuality(val label: String, val multiplier: Double, val description: String) {
        BUDGET("Budget Aftermarket", 0.6, "Lowest cost, standard durability"),
        STANDARD("Standard Quality", 1.0, "OEM-matching quality, recommended"),
        OEM("OEM Genuine Parts", 1.35, "Factory original parts, premium cost")
    }

    data class CostSimulation(
        val issueName: String,
        val standardHours: Double,
        val basePartsCost: Double,
        val difficultyLevel: String, // "Easy", "Moderate", "Expert", "Shop Only"
        val tipsToAvoidScams: String,
        
        // Sim State parameters
        val selectedQuality: PartsQuality = PartsQuality.STANDARD,
        val hourlyLaborRate: Int = 110
    ) {
        val partsCost: Double get() = (basePartsCost * selectedQuality.multiplier)
        val laborCost: Double get() = (standardHours * hourlyLaborRate)
        val totalCost: Double get() = partsCost + laborCost

        val formattedParts: String get() = "$${partsCost.roundToInt()}"
        val formattedLabor: String get() = "$${laborCost.roundToInt()}"
        val formattedTotal: String get() = "$${totalCost.roundToInt()}"
        val formattedRange: String get() {
            val min = (totalCost * 0.9).roundToInt()
            val max = (totalCost * 1.15).roundToInt()
            return if (min == max || min <= 0) "N/A" else "$$min - $$max"
        }
    }

    private val issueDatabase = mapOf(
        "Normal Healthy Sound Profile" to CostSimulation(
            issueName = "Normal Healthy Sound Profile",
            standardHours = 0.0,
            basePartsCost = 0.0,
            difficultyLevel = "Easy",
            tipsToAvoidScams = "No mechanical issues detected. Avoid letting shops charge you for unnecessary diagnostics or fluid flushes unless due on schedule."
        ),
        "Pulsating Spark Plug Anomaly" to CostSimulation(
            issueName = "Pulsating Spark Plug Anomaly",
            standardHours = 1.2,
            basePartsCost = 50.0,
            difficultyLevel = "Moderate",
            tipsToAvoidScams = "Spark plugs should take 1 to 1.5 hours of labor. Beware of shops claiming they need to replace ignition coils or wiring unless they show verified error codes."
        ),
        "Alternator Belt Squeal" to CostSimulation(
            issueName = "Alternator Belt Squeal",
            standardHours = 0.8,
            basePartsCost = 35.0,
            difficultyLevel = "Moderate",
            tipsToAvoidScams = "A standard serpentine belt replacement takes under an hour. Mechanics shouldn't charge more than 1 hour labor. Ensure they check the tensioner bearing first before replacing the entire belt."
        ),
        "Valve Train Ticking Noise" to CostSimulation(
            issueName = "Valve Train Ticking Noise",
            standardHours = 2.0,
            basePartsCost = 25.0,
            difficultyLevel = "Expert",
            tipsToAvoidScams = "Overhead valve clearances require manual adjustment. The book time is around 2 hours. Beware of mechanics suggesting complete engine head rebuilding for simple loose clearances."
        ),
        "Timing Chain Rattle" to CostSimulation(
            issueName = "Timing Chain Rattle",
            standardHours = 4.5,
            basePartsCost = 150.0,
            difficultyLevel = "Shop Only",
            tipsToAvoidScams = "This is a labor-intensive engine internals job taking 4 to 6 hours. Ask for a printed itemized estimate showing timing chain components and gasket kit separately to ensure parts prices are not inflated."
        ),
        "Cylinder Ignition / Spark Plug Service" to CostSimulation(
            issueName = "Cylinder Ignition / Spark Plug Service",
            standardHours = 1.2,
            basePartsCost = 55.0,
            difficultyLevel = "Moderate",
            tipsToAvoidScams = "Standard service. Shops might try to oversell a 'fuel system flush' at the same time. You can decline this if the vehicle runs smoothly after replacement."
        ),
        "Alternator / Tensioner Bearing Replacement" to CostSimulation(
            issueName = "Alternator / Tensioner Bearing Replacement",
            standardHours = 1.5,
            basePartsCost = 80.0,
            difficultyLevel = "Expert",
            tipsToAvoidScams = "Check if just the pulley can be replaced instead of the whole alternator, which is significantly cheaper. Ask if the estimate is for an OEM unit or aftermarket rebuilt part."
        ),
        "Overhead Valve / Tappet Adjustment" to CostSimulation(
            issueName = "Overhead Valve / Tappet Adjustment",
            standardHours = 2.0,
            basePartsCost = 25.0,
            difficultyLevel = "Expert",
            tipsToAvoidScams = "Mostly labor. If the valve cover gasket is not leaking, it doesn't always need replacement, but doing so during adjustment is standard practice."
        ),
        "Timing Chain / Tensioner Service" to CostSimulation(
            issueName = "Timing Chain / Tensioner Service",
            standardHours = 4.5,
            basePartsCost = 150.0,
            difficultyLevel = "Shop Only",
            tipsToAvoidScams = "Timing chain tensioners can wear early. Ensure the mechanic replaces the guides and gaskets at the same time to avoid paying for double labor later."
        ),
        "Clutch / CVT Belt Maintenance" to CostSimulation(
            issueName = "Clutch / CVT Belt Maintenance",
            standardHours = 1.0,
            basePartsCost = 60.0,
            difficultyLevel = "Moderate",
            tipsToAvoidScams = "CVT transmission belts or scooter clutches take about 1 hour. Do not accept recommendations for complete clutch kit replacements unless physical wear/glazing is demonstrated."
        )
    )

    fun simulate(
        issueName: String,
        quality: PartsQuality = PartsQuality.STANDARD,
        laborRate: Int = 110
    ): CostSimulation {
        val lower = issueName.lowercase()
        
        // Find matching entry or build a default estimate
        val matchedBase = issueDatabase.entries.firstOrNull { 
            lower.contains(it.key.lowercase()) || it.key.lowercase().contains(lower)
        }?.value ?: CostSimulation(
            issueName = issueName,
            standardHours = 1.0,
            basePartsCost = 45.0,
            difficultyLevel = "Moderate",
            tipsToAvoidScams = "Always ask the mechanic for the old parts they replaced. Standard labor for diagnostic evaluation is normally 1 hour."
        )

        return matchedBase.copy(
            selectedQuality = quality,
            hourlyLaborRate = laborRate
        )
    }

    fun getAllPredefinedIssues(): List<String> {
        return issueDatabase.keys.toList()
    }
}
