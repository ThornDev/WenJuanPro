package ai.wenjuanpro.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashSequenceGenerator
    @Inject
    constructor() {
        fun generate(dotsPositions: List<Int>): List<Int> = dotsPositions.shuffled()
    }
