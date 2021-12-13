package object_oriented_analysis_design.company

import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.dao.MainDAO
import object_oriented_analysis_design.util.Utils

case class Employee(
                       _id: String = Utils.getId,
                       lastName: String,
                       firstName: String,
                       middleName: Option[String] = None,
                       birthDate: String,
                       var stakeRates: Map[String, Set[StaffTable]] = Map.empty,
                       employmentDate: Option[String] = None,
                       passSeries: String,
                       passNumber: String,
                       snils: Option[String] = None
                   ) {
    def toMSA: MSA = Map(
        "_id" -> _id,
        "lastName" -> lastName,
        "firstName" -> firstName,
        "middleName" -> middleName.getOrElse(""),
        "birthDate" -> birthDate,
        "stakeRates" -> stakeRates.toList.map(sR => Map("companyName" -> sR._1, "tables" -> sR._2.map(_.toMSA))),
        "employmentDate" -> employmentDate.getOrElse(""),
        "passSeries" -> passSeries,
        "passNumber" -> passNumber,
        "snils" -> snils.getOrElse("")
    )

    def listStructuralUnits: List[String] = stakeRates.keys.toList

    def setStaffTable(structuralUnitName: String, staffTable: StaffTable): Unit = {
        val staffTableUpdated = stakeRates.get(structuralUnitName).map(_ + staffTable).getOrElse(Set(staffTable))
        stakeRates = stakeRates + (structuralUnitName -> staffTableUpdated)
    }

    def removeStaffTable(structuralUnitName: String, staffTable: StaffTable): Unit = {
        val staffTableUpdated = stakeRates.get(structuralUnitName).map(_ - staffTable).getOrElse(Set.empty)
        stakeRates = {
            if (staffTableUpdated.isEmpty) stakeRates - structuralUnitName
            else stakeRates + (structuralUnitName -> staffTableUpdated)
        }
    }

    def removeStaffTables(structuralUnitName: String): Unit = {
        stakeRates = stakeRates - structuralUnitName
    }

    def save: Unit = MainDAO.saveEmployee(this)

    def remove: Unit = MainDAO.removeEmployee(this._id)
}

object Employee {
    def fromMSA(msa: MSA) = Employee(
        msa.getOrElse("_id", Utils.getId).toString,
        msa("lastName").toString,
        msa("firstName").toString,
        msa.get("middleName").map(_.toString).flatMap(m => if (m.isEmpty) None else Some(m)),
        msa("birthDate").toString,
        msa("stakeRates").asInstanceOf[List[MSA]].map { sR =>
            sR("companyName").toString -> sR("tables").asInstanceOf[List[MSA]].map(StaffTable.fromMSA).toSet
        }.toMap,
        msa.get("employmentDate").map(_.toString).flatMap(d => if (d.isEmpty) None else Some(d)),
        msa("passSeries").toString,
        msa("passNumber").toString,
        msa.get("snils").map(_.toString)
    )

    def list: List[Employee] = MainDAO.findEmployees(Map.empty).toList

    def find(query: MSA): List[Employee] = MainDAO.findEmployees(query).toList

    def get(employeeId: String): Employee = find(Map("_id" -> employeeId)).head

    def getDefaultEmployee = Employee(
        Utils.getId,
        "Введите фамилию", "Введите имя", Some("Введите отчество (опционально)"),
        "Введите дату рождения", Map.empty, None,
        "Введите серию паспорта", "Введите номер паспорта", Some("Введите СНИЛС (опционально)")
    )
}