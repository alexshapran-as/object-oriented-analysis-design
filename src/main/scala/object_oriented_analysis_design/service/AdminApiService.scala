package object_oriented_analysis_design.service

import akka.http.scaladsl.server.{Directives, Route}
import object_oriented_analysis_design.api.web.HttpRouteUtils
import object_oriented_analysis_design.company.{DefaultStructuralUnit, Employee, StructuralUnit}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

object AdminApiService extends HttpRouteUtils with Directives {
    protected val logger: Logger = LoggerFactory.getLogger(getClass)


//    - ввод данных о новом сотруднике (полных данных или частичных: ФИО, год рождения и СНИЛС);
//    - вывод списка всех сотрудников (краткая информация: ФИО, год рождения, СНИЛС);
//    - поиск и вывод полных данных о сотруднике (сотрудниках) (поиск по ФИО, поиск по СНИЛС);
//    - поиск по СНИЛС и изменение некоторых данных о сотруднике (изменение ФИО, должности в структурном подразделении);
//    - поиск по СНИЛС и удаление сотрудника из БД;
//    - ввод данных о новом структурном подразделении (полностью или частично);
//    - вывод списка всех подразделения (названия);
//    - поиск по названию и изменение некоторых данных о структурном подразделении;
//    - поиск по названию и удаление подразделения из БД (сотрудники этого подразделения не удаляются, остаются в БД за штатом).

    def getRoute: Route =
        get("main_page") {
            getFromResource("web/admin_page.html")
        } ~
        respondWithJsonContentType {
            validateRequiredSession { session =>
                get("current_user") {
                    complete(getOkResponse(Map("username" -> session.username)))
                } ~
                pathPrefix("company") {
                    get("list") {
                        complete(getOkResponse(StructuralUnit.list.map(_.toMSA)))
                    } ~
                    post("create") {
                        DefaultStructuralUnit().save
                        complete(getOkResponse)
                    } ~
                    post("update") {
                        extractPostRequest { case (postStr, postMsa) =>
                            val company = StructuralUnit.fromMSA(postMsa)

                            // remove and save
                            Employee.find(Map("stakeRates.companyName" -> company.name)).foreach { employee =>
                                employee.removeStaffTables(company.name)
                                employee.save
                            }
                            // set
                            val staffTablesEmployees = company.staffTables.toList.flatMap { case (staffTable, employees) =>
                                employees.values.map { employee =>
                                    employee.setStaffTable(company.name, staffTable)
                                    (staffTable, employee)
                                }
                            }
                            // save
                            staffTablesEmployees.foreach(_._2.save)

                            // remove
                            company.removeEmployees()
                            // set
                            staffTablesEmployees.foreach { case (staffTable, employee) =>
                                company.setEmployee(staffTable, employee)
                            }
                            // save
                            company.save

                            complete(getOkResponse)
                        }
                    } ~
                    post("remove") {
                        extractPostRequest { case (postStr, postMsa) =>
                            val company = StructuralUnit.fromMSA(postMsa)
                            company.staffTables.foreach { case (_, idEmployees) =>
                                idEmployees.values.foreach { employee =>
                                    employee.removeStaffTables(company.name)
                                    employee.save
                                }
                            }
                            company.remove
                            complete(getOkResponse)
                        }
                   } ~
                   post("find_by_name") {
                       extractPostRequest { case (postStr, postMsa) =>
                           val companyName = postMsa("companyName").toString
                           complete(getOkResponse(List(StructuralUnit.getByName(companyName).toMSA)))
                       }
                   } ~
                   post("find_by_employee") {
                       extractPostRequest { case (postStr, postMsa) =>
                           val query = {
                               Map() ++
                               postMsa.get("lastName").map(lastName => ("staffTables.staff.lastName" -> lastName)) ++
                               postMsa.get("firstName").map(firstName => ("staffTables.staff.firstName" -> firstName)) ++
                               postMsa.get("middleName").map(middleName => ("staffTables.staff.middleName" -> middleName)) ++
                               postMsa.get("passSeries").map(passSeries => ("staffTables.staff.passSeries" -> passSeries)) ++
                               postMsa.get("passNumber").map(passNumber => ("staffTables.staff.passNumber" -> passNumber)) ++
                               postMsa.get("snils").map(snils => ("staffTables.staff.snils" -> snils))
                           }
                           complete(getOkResponse(StructuralUnit.find(query).map(_.toMSA)))
                       }
                   }
                } ~
                pathPrefix("employee") {
                    get("list") {
                        val e = Employee.list.map(_.toMSA)
                        complete(getOkResponse(e))
                    } ~
                    post("create") {
                        Employee.getDefaultEmployee.save
                        complete(getOkResponse)
                    } ~
                    post("update") {
                        extractPostRequest { case (postStr, postMsa) =>
                            val employee = Employee.fromMSA(postMsa)
                            val employeeFromDB = Employee.get(employee._id)

                            // remove and save
                            employeeFromDB.stakeRates.foreach { case (companyName, staffTables) =>
                                val company = StructuralUnit.getByName(companyName)
                                staffTables.foreach { staffTable =>
                                    company.removeEmployee(staffTable, employeeFromDB._id)
                                    company.save
                                }
                            }

                            // set and save
                            employee.stakeRates.foreach { case (companyName, staffTables) =>
                                val company = StructuralUnit.getByName(companyName)
                                staffTables.foreach { staffTable =>
                                    company.setEmployee(staffTable, employee)
                                    company.save
                                }
                            }

                            // save
                            employee.save

                            complete(getOkResponse)
                        }
                    } ~
                    post("remove") {
                        extractPostRequest { case (postStr, postMsa) =>
                            val employee = Employee.fromMSA(postMsa)
                            employee.stakeRates.foreach { case (companyName, staffTables) =>
                                val company = StructuralUnit.getByName(companyName)
                                staffTables.foreach { staffTable =>
                                    company.removeEmployee(staffTable, employee._id)
                                    company.save
                                }
                            }
                            employee.remove
                            complete(getOkResponse)
                        }
                    } ~
                    post("find_by_company") {
                        extractPostRequest { case (postStr, postMsa) =>
                            val companyName = postMsa("companyName").toString
                            val employees = Employee.find(Map("stakeRates.companyName" -> companyName))
                            complete(getOkResponse(employees.map(_.toMSA)))
                        }
                    } ~
                    post("find_by_query") {
                        extractPostRequest { case (postStr, postMsa) =>
                            val query = {
                                Map() ++
                                postMsa.get("lastName").map(lastName => ("lastName" -> lastName)) ++
                                postMsa.get("firstName").map(firstName => ("firstName" -> firstName)) ++
                                postMsa.get("middleName").map(middleName => ("middleName" -> middleName)) ++
                                postMsa.get("passSeries").map(passSeries => ("passSeries" -> passSeries)) ++
                                postMsa.get("passNumber").map(passNumber => ("passNumber" -> passNumber)) ++
                                postMsa.get("snils").map(snils => ("snils" -> snils))
                            }
                            complete(getOkResponse(Employee.find(query).map(_.toMSA)))
                        }
                    }
                }
            }
        }
}
