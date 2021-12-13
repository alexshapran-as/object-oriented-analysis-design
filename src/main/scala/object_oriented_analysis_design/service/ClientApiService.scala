package object_oriented_analysis_design.service

import akka.http.scaladsl.server.{Directives, Route}
import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.api.web.{HttpRouteUtils, UserSessionData}

object ClientApiService extends HttpRouteUtils with Directives {
    def getRoute: Route =
        get("main_page") {
            getFromResource("web/client_page.html")
        } ~
        respondWithJsonContentType {
            validateRequiredSession { session =>
                get("current_user") {
                    Shop.customer = Customer(UserSessionData(session.username, session.groups))
                    complete(getOkResponse(Map("username" -> session.username)))
                } ~
                pathPrefix("target") {
                    post("insert") {
                        extractPostRequest { case (postStr, targetMSA) =>
                            val target = Target.fromMSA(targetMSA)
                            Shop.targets = Shop.targets + (target.name -> target)
                            complete(getOkResponse)
                        }
                    } ~
                    get("all") {
                        complete {
                            getOkResponse(
                                Shop.targets.values.map { target =>
                                    val targetCut = target.toMSACut
                                    s"""
                                       |Товар: "${targetCut("name")}"
                                       |Цена за единицу товара: ${targetCut("price")}
                                       |_________________________________________
                                       |""".stripMargin
                                }.mkString("\n")
                            )
                        }
                    } ~
                    post("get") {
                        extractPostRequest { case (postStr, targetMSA) =>
                            complete(
                                getOkResponse(
                                    Shop.targets.get(targetMSA("name").toString).map { target =>
                                        s"""
                                           |Товар: "${target.name}"
                                           |Количество единиц: ${target.amount}
                                           |Цена за единицу товара: ${target.price}
                                           |Описание: ${target.description}
                                           |_________________________________________
                                           |""".stripMargin
                                    }.getOrElse("Товар не найден. Выберите один из доступных товаров")
                                )
                            )
                        }
                    }
                } ~
                pathPrefix("order") {
                    post("create") {
                        val orderId = Shop.customer.orders.length + 1
                        Shop.customer = Shop.customer.copy(
                            orders = Shop.customer.orders :+ Order(orderId, List.empty, 0)
                        )
                        complete(getOkResponse(s"Для пользователя ${Shop.customer.session.username} Заказ №$orderId создан"))
                    } ~
                    get("all") {
                        complete(
                            getOkResponse(
                                Shop.customer.orders.map(order =>
                                    s"[${order._id}] Заказ №${order._id}"
                                ).mkString("\n")
                            )
                        )
                    } ~
                    post("insert") {
                        extractPostRequest { case (postStr, postMSA) =>
                            val orderId = postMSA("orderId").toString.toInt
                            Shop.customer.orders.zipWithIndex.find(_._1._id == orderId) match {
                                case None =>
                                    complete(getErrorResponse(-1, "Заказ не найден. Выберите один из существующих"))
                                case Some((order, orderIndex)) =>
                                    val targetName = postMSA("targetName").toString
                                    Shop.targets.get(targetName) match {
                                        case None =>
                                            complete(getErrorResponse(-1, "Товар не найден. Введите название доступного товара"))
                                        case Some(target) =>
                                            val amountToBuy = postMSA("targetAmountToBuy").toString.toLong
                                            if (target.amount < amountToBuy) {
                                                complete(getErrorResponse(-1, "Вы не можете добавить больше позиций товара, чем имеется. Добавьте товары заново"))
                                            } else {
                                                Shop.targets = Shop.targets - targetName + (
                                                    targetName -> target.copy(
                                                        amount = target.amount - amountToBuy
                                                    )
                                                )
                                                val targetIndex = {
                                                    order.targets
                                                        .zipWithIndex
                                                        .find(_._1.name == targetName)
                                                        .map(_._2)
                                                        .getOrElse(-1)
                                                }
                                                val targetsUpdated = {
                                                    if (targetIndex != -1) order.targets.updated(targetIndex, order.targets(targetIndex).copy(amount = order.targets(targetIndex).amount + amountToBuy))
                                                    else order.targets :+ target.copy(amount = amountToBuy)
                                                }
                                                val orderUpdated = order.copy(
                                                    targets = targetsUpdated,
                                                    amount = targetsUpdated.map(_.amount).sum
                                                )
                                                Shop.customer = Shop.customer.copy(
                                                    orders = Shop.customer.orders.updated(orderIndex, orderUpdated)
                                                )
                                                complete(getOkResponse("Единицы товара были добавлены в заказ"))
                                            }
                                    }
                            }
                        }
                    } ~
                    post("get") {
                        extractPostRequest { case (postStr, postMSA) =>
                            val orderId = postMSA("orderId").toString.toInt
                            Shop.customer.orders.find(_._id == orderId) match {
                                case None =>
                                    complete(getErrorResponse(-1, "Заказ не найден. Выберите один из существующих"))
                                case Some(order) =>
                                    complete(
                                        getOkResponse(
                                            s"""
                                               |Заказ №${order._id}
                                               |Товары:
                                               |${
                                                    order.targets.map { target =>
                                                        s"""
                                                           |Товар: "${target.name}"
                                                           |Количество единиц: ${target.amount}
                                                           |Цена за единицу товара: ${target.price}
                                                           |Описание: ${target.description}
                                                           |_________________________________________
                                                           |""".stripMargin
                                                    }.mkString("\n")
                                                }
                                               |Количество товаров: ${order.amount}
                                               |Оплачено: ${if (order.payed) "Да" else "Нет"}
                                            """.stripMargin
                                        )
                                    )
                            }
                        }
                    } ~
                    post("pay") {
                        extractPostRequest { case (postStr, postMSA) =>
                            val orderId = postMSA("orderId").toString.toInt
                            Shop.customer.orders.zipWithIndex.find(_._1._id == orderId) match {
                                case None =>
                                    complete(getErrorResponse(-1, "Заказ не найден. Выберите один из существующих"))
                                case Some((order, orderIndex)) =>
                                    if (order.payed) {
                                        complete(getErrorResponse(-1, s"Заказ №${orderId} уже был успешно оплачен"))
                                    } else {
                                        val orderUpdated = order.copy(payed = true)
                                        Shop.customer = Shop.customer.copy(
                                            orders = Shop.customer.orders.updated(orderIndex, orderUpdated)
                                        )
                                        complete(getOkResponse(s"Заказ №${orderId} успешно оплачен"))
                                    }
                            }
                        }
                    }
                }
            }
        }

    // DOMAIN

    object Shop {
        var targets: Map[String, Target] = Map.empty // name -> Target()
        var customer: Customer = null
    }

    case class Target(
                         name: String,
                         price: Long,
                         amount: Long,
                         description: String = ""
                     ) {
        def toMSA = Map(
            "name" -> name,
            "price" -> price,
            "amount" -> amount,
            "description" -> description
        )

        def toMSACut = Map(
            "name" -> name,
            "price" -> price
        )
    }

    object Target {
        def fromMSA(msa: MSA) = Target(
            msa("name").toString,
            msa("price").toString.toLong,
            msa("amount").toString.toLong,
            msa("description").toString
        )
    }

    case class Order(
                        _id: Int,
                        targets: List[Target],
                        amount: Long,
                        payed: Boolean = false
                    )

    case class Customer(
                           session: UserSessionData,
                           orders: List[Order] = List.empty
                       )
}
