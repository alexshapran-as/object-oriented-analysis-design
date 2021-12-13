package object_oriented_analysis_design.company

import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.util.Utils

case class DefaultStructuralUnit(
                                    _id: String = Utils.getId,
                                    `type`: StructuralUnitTypes.Value = StructuralUnitTypes.DEFAULT,
                                    name: String = "Впишите название подразделения",
                                    purpose: String = "Впишите цель подразделения",
                                    var staffTables: Map[StaffTable, Map[String, Employee]] = Map.empty
                                ) extends StructuralUnit

object DefaultStructuralUnit {
    def fromMSA(msa: MSA) = DefaultStructuralUnit(
        msa("_id").toString,
        StructuralUnitTypes.withName(msa("type").toString),
        msa("name").toString,
        msa("purpose").toString,
        msa("staffTables").asInstanceOf[List[MSA]].map { staffTable =>
            val table = StaffTable.fromMSA(staffTable("table").asInstanceOf[MSA])
            val staff = staffTable("staff").asInstanceOf[List[MSA]].map { s =>
                val e = Employee.fromMSA(s)
                e._id -> e
            }
            table -> staff.toMap
        }.toMap
    )
}
