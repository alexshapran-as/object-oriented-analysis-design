package object_oriented_analysis_design.company

import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.dao.MainDAO
import org.slf4j.LoggerFactory

trait StructuralUnit {
    val _id: String
    val `type`: StructuralUnitTypes.Value
    val name: String
    val purpose: String
    var staffTables: Map[StaffTable, Map[String, Employee]]

    protected val logger = LoggerFactory.getLogger(getClass)

    def toMSA = Map(
        "_id" -> _id,
        "type" -> `type`.toString,
        "name" -> name,
        "purpose" -> purpose,
        "staffTables" -> staffTables.toList.map(sT => Map("table" -> sT._1.toMSA, "staff" -> sT._2.values.map(_.toMSA)))
    )

    def setEmployee(staffTable: StaffTable, employee: Employee): Unit = {
        val idEmployee = Map(employee._id -> employee)
        val staffTablesItem = staffTable -> staffTables.get(staffTable).map(_ ++ idEmployee).getOrElse(idEmployee)
        staffTables = staffTables + staffTablesItem
    }

    def removeEmployee(staffTable: StaffTable, employeeId: String): Unit = {
        staffTables.get(staffTable).map(_ - employeeId) match {
            case Some(idsEmployeesUpdated) if idsEmployeesUpdated.isEmpty =>  staffTables = staffTables - staffTable
            case Some(idsEmployeesUpdated) => staffTables = staffTables + (staffTable -> idsEmployeesUpdated)
            case _ => logger.info("StructuralUnit[removeEmployee] error: not found staffTable")
        }
    }

    def removeEmployees() = {
        staffTables = Map.empty
    }

    def listEmployees: Set[Employee] = staffTables.values.flatMap(_.values).toSet

    def findEmployees(employeeSearchQuery: MSA): Set[Employee] = {
        listEmployees
            .map(_.toMSA.toSet)
            .filter(employeeSearchQuery.toSet subsetOf _)
            .map(employeeMsaAsSet => Employee.fromMSA(employeeMsaAsSet.toMap))
    }

    def tryToGetEmployee(employeeId: String): Option[Employee] = listEmployees.find(_._id == employeeId)

    def getEmployee(employeeId: String): Employee = tryToGetEmployee(employeeId).getOrElse {
        sys.error(s"В подразделении $name не найден сотрудник с _id: $employeeId")
    }

    def save: Unit = MainDAO.saveStructuralUnit(this)

    def remove: Unit = MainDAO.removeStructuralUnit(this._id)
}

object StructuralUnit {
    def fromMSA(msa: MSA): StructuralUnit = StructuralUnitTypes.withName(msa("type").toString) match {
        case StructuralUnitTypes.BOOKKEEPING => Bookkeeping.fromMSA(msa)
        case StructuralUnitTypes.DEFAULT => DefaultStructuralUnit.fromMSA(msa)
    }

    def list: List[StructuralUnit] = MainDAO.findStructuralUnits(Map.empty).toList

    def find(query: MSA): List[StructuralUnit] = MainDAO.findStructuralUnits(query).toList

    def getByName(name: String): StructuralUnit = find(Map("name" -> name)).head
}

object StructuralUnitTypes extends Enumeration {
    val BOOKKEEPING, DEFAULT = Value
}