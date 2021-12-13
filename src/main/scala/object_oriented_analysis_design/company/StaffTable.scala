package object_oriented_analysis_design.company

import object_oriented_analysis_design.MSA

case class StaffTable(position: String, stakeRate: Double) {
    def toMSA = Map(
        "position" -> position,
        "stakeRate" -> stakeRate
    )
}

object StaffTable {
    def fromMSA(msa: MSA) = StaffTable(
        msa("position").toString, msa("stakeRate").toString.toDouble
    )
}
