package object_oriented_analysis_design.company

import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.util.Utils

case class Bookkeeping(
                          _id: String = Utils.getId,
                          `type`: StructuralUnitTypes.Value = StructuralUnitTypes.BOOKKEEPING,
                          name: String,
                          purpose: String,
                          var staffTables: Map[StaffTable, Map[String, Employee]] = Map.empty
                      ) extends StructuralUnit {
}

object Bookkeeping {
    def fromMSA(msa: MSA) = Bookkeeping(
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
