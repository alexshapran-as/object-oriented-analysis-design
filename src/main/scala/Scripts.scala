import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.api.web.UserSessionData
import object_oriented_analysis_design.authenticator.{AuthApi, HashApi, Roles}
import object_oriented_analysis_design.company.{Bookkeeping, Employee, StaffTable}
import object_oriented_analysis_design.dao.MainDAO.getClass
import object_oriented_analysis_design.dao.{MainDAO, MongoUtils}
import object_oriented_analysis_design.service.AdminApiService.validateRequiredSession
import object_oriented_analysis_design.util.Utils
import org.apache.poi.xwpf.usermodel.{IBody, IBodyElement, IRunBody, XWPFDocument, XWPFParagraph, XWPFRun, XWPFTable}
import org.apache.xmlbeans.impl.soap.Node
import org.apache.xmlbeans.{SchemaType, XmlCursor, XmlObject, XmlOptions}
import org.openxmlformats.schemas.drawingml.x2006.main.CTTable
import org.openxmlformats.schemas.wordprocessingml.x2006.main.{CTBody, CTR, CTString, CTTbl, CTTblPr}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayOutputStream, FileInputStream, InputStream}
import java.math.BigInteger
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.TimeZone
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.io.StdIn

object ShopScript {

    object Utils {
        implicit val formats = org.json4s.DefaultFormats.withLong.withDouble.withStrictOptionParsing

        def getId: String = org.bson.types.ObjectId.get().toString

        // -------------------------------------------------------------------------------------------------------------------
        // Методы работы с датой и временем
        // -------------------------------------------------------------------------------------------------------------------
        val MINUTE = 60 * 1000L
        val HOUR = MINUTE * 60
        val DAY = HOUR * 24

        val defaultTimeZoneStr = "GMT+3"
        val defaultDateFormat = "yyyy-MM-dd"
        val defaultTimeFormat = "yyyy-MM-dd HH:mm:ss"
        val defaultTimeZone = TimeZone.getTimeZone(defaultTimeZoneStr)

        def millisToFormattedDate(t: Long, format: String = defaultTimeFormat, timeZone: String = defaultTimeZoneStr) = {
            val sdf = new SimpleDateFormat(format)
            sdf.setTimeZone(TimeZone.getTimeZone(timeZone))
            sdf.format(new java.util.Date(t))
        }

        def formattedDateToMillis(date: String, format: String = defaultDateFormat, timeZone: String = defaultTimeZoneStr): Long = {
            val sdf = new SimpleDateFormat(format)
            sdf.setTimeZone(TimeZone.getTimeZone(timeZone))
            sdf.parse(date).getTime
        }
    }

    case class Shop(
                       _id: String = Utils.getId,
                       name: String,
                       address: Address,
                       productsColl: Map[String, Array[Product]] = Map.empty
                   ) {
        def setProduct(product: Product): Shop = {
            val productsUpdated = productsColl.get(product.name).map { productsExisting =>
                productsExisting :+ product
            }.getOrElse(Array(product))
            this.copy(productsColl = productsColl + (product.name -> productsUpdated))
        }

        def tryToGetProducts(name: String): Option[Array[Product]] =
            productsColl.get(name)

        def getProducts(name: String): Array[Product] =
            tryToGetProducts(name).getOrElse(sys.error(s"Products with name $name not found in shop ${this.name}"))

        def tryToGetProduct(product: Product): Option[Product] =
            productsColl.get(product.name).flatMap(products => products.find(_.equals(product)))

        def getProduct(product: Product): Product =
            tryToGetProduct(product).getOrElse(sys.error(s"Product ${product.toString} not found in shop ${this.name}"))
    }

    case class Product(
                          _id: String = Utils.getId,
                          name: String,
                          manufactureDate: Long = System.currentTimeMillis,
                          expirationDate: Option[Long] = None,
                          shopsColl: Map[String, Array[Shop]] = Map.empty
                      ) {
        def setShop(shop: Shop): Product = {
            val shopsUpdated = shopsColl.get(shop.name).map { shopsExisting =>
                shopsExisting :+ shop
            }.getOrElse(Array(shop))
            this.copy(shopsColl = shopsColl + (shop.name -> shopsUpdated))
        }

        def tryToGetShops(name: String): Option[Array[Shop]] =
            shopsColl.get(name)

        def getShops(name: String): Array[Shop] =
            tryToGetShops(name).getOrElse(sys.error(s"Shops with name $name not found for product ${this.name}"))

        def tryToGetShop(shop: Shop): Option[Shop] =
            shopsColl.get(shop.name).flatMap(shops => shops.find(_.equals(shop)))

        def getShop(shop: Shop): Shop =
            tryToGetShop(shop).getOrElse(sys.error(s"Shop ${shop.toString} not found fro product ${this.name}"))
    }

    case class Address(
                          fias: Option[String] = None,
                          kladr: Option[String] = None,
                          regionName: Option[String] = None,
                          areaName: Option[String] = None,
                          cityName: Option[String] = None,
                          settlementName: Option[String] = None,
                          streetName: Option[String] = None,
                          house: Option[String] = None,
                          building: Option[String] = None,
                          construction: Option[String] = None,
                          flat: Option[String] = None,
                          postalCode: Option[String] = None
                      )

    def main(args: Array[String]): Unit = {
        var ashanShops = List(
            Shop(name = "Ашан", address = Address(cityName = Some("Москва"))),
            Shop(name = "Ашан", address = Address(cityName = Some("Санкт-Петербург"))),
            Shop(name = "Ашан", address = Address())
        )
        var magnitShops = List(
            Shop(name = "Магнит", address = Address(cityName = Some("Москва"))),
            Shop(name = "Магнит", address = Address(cityName = Some("Санкт-Петербург")))
        )
        var marshmallow1 = Product(name = "Зефир")
        var marshmallow2 = Product(name = "Зефир", expirationDate = Some(System.currentTimeMillis))
        var milk = Product(name = "Молоко")

        ashanShops = ashanShops.map { ashanShop =>
            marshmallow1 = marshmallow1.setShop(ashanShop)
            marshmallow2 = marshmallow2.setShop(ashanShop)
            milk = milk.setShop(ashanShop)
            ashanShop.setProduct(marshmallow1).setProduct(marshmallow2).setProduct(milk)
        }

        magnitShops = magnitShops.map { magnitShop =>
            milk = milk.setShop(magnitShop)
            magnitShop.setProduct(milk)
        }

        val marshmallowsInMoscowAshan = ashanShops.find(_.address.cityName.contains("Москва")).map { moscowAshan =>
            moscowAshan.getProducts("Зефир").map { product =>
                "Продукт: " + product.name +
                    "; Дата изготовления: " + Utils.millisToFormattedDate(product.manufactureDate) +
                    "; Годен до: " + product.expirationDate.map(Utils.millisToFormattedDate(_)).getOrElse("Не указано") +
                    "; Идентификатор: " + product._id
            }.mkString("\n")
        }.getOrElse("")
        println("В Московском Ашане следующие виды зефира:\n" + marshmallowsInMoscowAshan)

        val shopsWithMilk = milk.shopsColl.map { shops =>
            "Сеть магазинов: " + shops._1 + "\n" +
                shops._2.map { shop =>
                    "Магазин: " + shop.name + "; Адрес: " + shop.address.cityName.getOrElse("Не указано") +
                        "; Идентификатор: " + shop._id
                }.mkString("\n")
        }.mkString("\n")
        println("Молоко представлено в следующих сетях магазинов:\n" + shopsWithMilk)
    }
}

//object StructuralUnitsScripts {
//    def main(args: Array[String]): Unit = {
//        val employee = Employee(
//            "Иванов", "Иван", Some("Иванович"), "1998-08-12",
//            passSeries = "1234", passNumber = "123456", snils = Some("1234567890")
//        )
//        employee.setStaffTable("Шиномонтаж №1", StaffTable("мойщик окон", 1.0 / 2))
//
//        val company = Bookkeeping(name = "Шиномонтаж №1", purpose = "шиномонтаж, ремонт автомобилей")
//        company.setEmployee(StaffTable("мойщик окон", 1.0 / 2), employee)
//
//        employee.save
//        company.save
//    }
//}

object DocxUpdaterScript {
    def main(args: Array[String]): Unit = {
        val bytes = DocxTemplate(
            new FileInputStream("/Users/j/Desktop/pisos.docx"),
            Map("prCode1" -> "niga", "prCode10" -> "!!!", "prCode12" -> "hello", "passSer" -> "серия", "passNum" -> "номер"),
            Map("prCode1" -> Map("type" -> "table", "rows" -> 1, "cols" -> 4, "split" -> "chars", "pos" -> Map("x" -> "5000", "y" -> "-400")))
        ).asNewByteArray()
        Files.write(Paths.get("/Users/j/Desktop/pisos-processed.docx"), bytes)
    }

    case class DocxTemplate(
                               inputStream: InputStream,
                               params: Map[String, Any],
                               mapParams: Map[String, Any]
                           ) {
        val doc = new XWPFDocument(inputStream)

        def asNewByteArray(): Array[Byte] = {
            doc.getHeaderList.asScala.toList.foreach(_.getBodyElements.asScala.foreach(replaceHandler))
            doc.getFooterList.asScala.toList.foreach(_.getBodyElements.asScala.foreach(replaceHandler))
            doc.getBodyElements.asScala.toList.foreach(replaceHandler)

            val baos = new ByteArrayOutputStream()
            doc.write(baos)
            doc.close()
            val barr = baos.toByteArray
            baos.close()

            barr
        }

        private def replaceHandler(elem: IBodyElement): Unit = elem match {
            case paragraph: XWPFParagraph =>
                replaceKeysInParagraph(paragraph, params, mapParams)
            case table: XWPFTable =>
                table.getRows.asScala foreach { row =>
                    row.getTableCells.asScala foreach { cell =>
                        cell.getParagraphs.asScala foreach { paragraph =>
                            replaceKeysInParagraph(paragraph, params, mapParams)
                        }
                        cell.getTables.asScala.toList.foreach(replaceHandler)
                    }
                }
        }

        private def replaceKeysInParagraph(paragraph: XWPFParagraph, params: Map[String, Any], mapParams: Map[String, Any]): Unit = {
            val textBoxesCursor = paragraph.getCTP.newCursor
            textBoxesCursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//*/w:txbxContent/w:p/w:r")
            replaceKeysInParagraphHandler(textBoxesCursor, paragraph, params, mapParams)

            val textCursor = paragraph.getCTP.newCursor
            textCursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' ./w:r")
            replaceKeysInParagraphHandler(textCursor, paragraph, params, mapParams)
        }

        def replaceKeysInParagraphHandler(cursor: XmlCursor, paragraph: XWPFParagraph, params: Map[String, Any], mapParams: Map[String, Any]): Unit = {
            val objects = getObjectsFromCursor(cursor)

            //            var i = 0
            //            while (cursor.hasNextSelection) {
            //                cursor.toNextSelection
            //                i = i + 1
            //                val obj = cursor.getObject
            //
            //                val ctr = CTR.Factory.parse(obj.xmlText)
            //                val run = new XWPFRun(ctr, paragraph.asInstanceOf[IRunBody])
            //                val text = run.toString
            //
            //                if (text.nonEmpty) {
            //                    val paramFullyFoundInText = params.filter(p => text.contains(p._1))
            //                    if (paramFullyFoundInText.nonEmpty) {
            //                        val ctTable = CTTbl.Factory.newInstance()
            //                        val table = new XWPFTable(ctTable, paragraph.getBody)
            //                        table.createRow().createCell().setText("Sosi2")
            //                        paragraph.getBody.insertTable(i, table)
            //                        obj.set(table.getCTTbl)
            ////                        obj(ctTable)
            //
            ////                        val nestedTable =   paragraph.getBody.insertNewTbl(cursor.newCursor())
            ////                        val rowOfNestedTable = nestedTable.getRow(0).getCell(0).setText("Sosi")
            //                    }
            //                }
            //            }

            // СЧИТАЕМ, ЧТО СЛОВА ОТДЕЛЕННЫЕ ПРОБЕЛОМ ТОЧНО НАХОДЯТСЯ В РАЗНЫХ ОБЪЕКТАХ
            // замена полностью найденных параметров и запоминание последовательных объектов с найденными кусочками параметров
            // формирование текста с кусочками параметров, пока в тексте не найдется полный параметр
            // замена объектов с кусочками параметров на один объект с полным текстом и замененным параметром
            // если последовательность кусочков параметров нарушается, то эти кусочки уже никогда не составят полный параметр
            objects.foldLeft((List.empty[XmlObject], "")) { case ((objectsToReplaceWithAcc, mergedText), obj) =>
                val ctr = CTR.Factory.parse(obj.xmlText)
                val run = new XWPFRun(ctr, paragraph.asInstanceOf[IRunBody])
                val text = run.toString

                if (text.isEmpty) (List(), "")
                else {
                    // TODO: тут ошибка - prCode1 и prCode10 воспринял, как prCode1
                    val paramsFullyFoundInText = params.filter(p => text.contains(p._1))
                    if (paramsFullyFoundInText.nonEmpty) {
                        paramsFullyFoundInText.foreach {
                            case paramFullyFoundInText if !mapParams.contains(paramFullyFoundInText._1) =>
                                run.setText(paramsFullyFoundInText.foldLeft(mergedText + text)((txt, p) => txt.replace(p._1, p._2.toString)), 0)
                                obj.set(run.getCTR)

                            case paramFullyFoundInText if mapParams(paramFullyFoundInText._1).asInstanceOf[Map[String, Any]]("type").toString == "table" =>
                                val settings =  mapParams(paramFullyFoundInText._1).asInstanceOf[Map[String, Any]]
                                val table = paragraph.getBody.insertNewTbl(paragraph.getCTP.newCursor())

                                val collsText = if (settings("split").toString == "chars") paramFullyFoundInText._2.toString.toCharArray else Array.empty[Char]
                                collsText.foreach { c =>
                                    table.getRow(0).addNewTableCell().setText(c.toString)
                                }


                                val tableProperties: XmlObject = table.getCTTbl.getTblPr.asInstanceOf[XmlObject]

                                var tablePosPropsCursor: XmlCursor = tableProperties.newCursor()  // Create a cursor at the element
                                tablePosPropsCursor.toNextToken              // Move cursor after the tblPr tag
                                tablePosPropsCursor.insertElement("tblpPr", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
                                tablePosPropsCursor.toPrevSibling // Now go to the tblpPr
                                val posProps = tablePosPropsCursor.getObject // Get the tblpPr object
                                tablePosPropsCursor.dispose()
                                tablePosPropsCursor = posProps.newCursor() // Now our cursor is inside the second tblpPr
                                tablePosPropsCursor.toNextToken
                                tablePosPropsCursor.insertAttributeWithValue("tblpX", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", settings("pos").asInstanceOf[Map[String, String]]("x"))
                                tablePosPropsCursor.insertAttributeWithValue("tblpY", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", settings("pos").asInstanceOf[Map[String, String]]("y"))
                                tablePosPropsCursor.insertAttributeWithValue("leftFromText", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "10")
                                tablePosPropsCursor.insertAttributeWithValue("rightFromText", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "10")
                                tablePosPropsCursor.insertAttributeWithValue("vertAnchor", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "text")
                                tablePosPropsCursor.insertAttributeWithValue("horzAnchor", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "page")
                                tablePosPropsCursor.insertAttributeWithValue("tblOverlap", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "never")
                                tablePosPropsCursor.dispose()

                                var tableOverlapPropsCursor: XmlCursor = tableProperties.newCursor()  // Create a cursor at the element
                                tableOverlapPropsCursor.toNextToken              // Move cursor after the tblPr tag
                                tableOverlapPropsCursor.insertElement("tblOverlap", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
                                tableOverlapPropsCursor.toPrevSibling // Now go to the tblOverlap
                                val overlapProps = tableOverlapPropsCursor.getObject // Get the tblOverlap object
                                tableOverlapPropsCursor.dispose()
                                tableOverlapPropsCursor = overlapProps.newCursor() //Now our cursor is inside the second tblpPr
                                tableOverlapPropsCursor.toNextToken
                                tableOverlapPropsCursor.insertAttributeWithValue("val", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "never")
                                tableOverlapPropsCursor.dispose()

                                run.setText("", 0)
                                obj.set(run.getCTR)
                                objectsToReplaceWithAcc.foreach(_.set(null))
                        }

                        (List(), "")
                    } else {
                        val paramsPossible = params.collect { case p if p._1.contains(mergedText + text) => p }
                        if (paramsPossible.isEmpty) (List(), "")
                        else {
                            paramsPossible.find(p => (mergedText + text).contains(p._1)) match {
                                case None => (objectsToReplaceWithAcc :+ obj, mergedText + text)
                                case Some(paramExact) =>
                                    val mergedTextWithKeys = (mergedText + text).replaceAll(paramExact._1, paramExact._2.toString)
                                    run.setText(mergedTextWithKeys, 0)
                                    obj.set(run.getCTR)
                                    objectsToReplaceWithAcc.foreach(_.set(null))
                                    (List(), "")
                            }
                        }
                    }
                }
            }
        }

        def getObjectsFromCursor(cursor: XmlCursor): List[XmlObject] = {
            var objects = List.empty[XmlObject]
            while (cursor.hasNextSelection) {
                cursor.toNextSelection
                objects = objects :+ cursor.getObject
            }
            objects
        }
    }
}

object DZ3 {

    protected val logger: Logger = LoggerFactory.getLogger(getClass)

    def main(args: Array[String]): Unit = {
        println("*** Контрольное задание 3. Разработка системы с N-арной ассоциацией между классами ***")
        authFlow()
        shopFlow()
    }

    implicit val formats = org.json4s.DefaultFormats

    var targets: Map[String, Target] = Map.empty // name -> Target()
    var customer: Customer = null

    def authFlow(): Unit = {
        println("Авторизация")
        println("Выберите действие:")
        println(
            """
              |[1] вход;
              |[2] регистрация;
              |[3] выход.
              |""".stripMargin
        )
        StdIn.readLine() match {
            case "1" =>
                val session = auth()
                customer = Customer(session)
            case "2" =>
                val session = register()
                customer = Customer(session)
            case "3" =>
                println("До свидания!")
                sys.exit()
            case _ =>
                println("Некорректный ввод. Введите цифру от 1 до 3")
                authFlow()
        }
    }

    @tailrec
    def auth(attempt: Int = 3): UserSessionData = {
        if (attempt > 0) {
            println("Введите логин:")
            val userName = StdIn.readLine()
            println("Введите пароль:")
            val password = StdIn.readLine()
            AuthApi.authorize(userName, password) match {
                case Some(roles) =>
                    MongoUtils.closeMongoConnection()
                    UserSessionData(userName, roles)
                case None =>
                    println("Логин или пароль неверные")
                    auth(attempt - 1)
            }
        } else {
            sys.error("Было сделано максимальное число попыток авторизации")
        }
    }

    def register(attempt: Int = 3): UserSessionData = {
        if (attempt > 0) {
            println("Введите логин:")
            val userName = StdIn.readLine()
            println("Введите пароль:")
            val password = StdIn.readLine()
            AuthApi.register(userName, password) match {
                case None =>
                    MongoUtils.closeMongoConnection()
                    UserSessionData(userName, List(Roles.ADMIN.toString))
                case Some(errMsg) =>
                    println(s"Ошибка: ${errMsg}")
                    register(attempt - 1)
            }
        } else {
            sys.error("Было сделано максимальное число попыток регистрации")
        }
    }

    @tailrec
    def shopFlow(): Unit = {
        println("Выберите действие:")
        println(
            """
              |[1] ввод данных о товаре в БД;
              |[2] просмотр всех товаров (краткие данные);
              |[3] просмотр подробной информации о товаре;
              |[4] создание заказа;
              |[5] добавление товара в заказ;
              |[6] просмотр заказа;
              |[7] оплата заказа;
              |[8] выход
              |""".stripMargin)
        StdIn.readLine() match {
            case "1" =>
                var targetMSA: MSA = Map.empty
                println("Введите данные для заведения или обновления товара")
                println("Название:")
                targetMSA = targetMSA + ("name" -> StdIn.readLine())
                println("Цена за единицу товара:")
                targetMSA = targetMSA + ("price" -> StdIn.readLong())
                println("Количество единиц товара:")
                targetMSA = targetMSA + ("amount" -> StdIn.readLong())
                println("Описание товара:")
                targetMSA = targetMSA + ("description" -> StdIn.readLine())
                val target = Target.fromMSA(targetMSA)
                targets = targets + (target.name -> target)
                shopFlow()
            case "2" =>
                println(
                    targets.values.map { target =>
                        val targetCut = target.toMSACut
                        s"""
                           |Товар: "${targetCut("name")}"
                           |Цена за единицу товара: ${targetCut("price")}
                           |_________________________________________
                           |""".stripMargin
                    }.mkString("\n")
                )
                shopFlow()
            case "3" =>
                println("Введите название товара:")
                val targetName = StdIn.readLine()
                println(
                    targets.get(targetName).map { target =>
                        s"""
                           |Товар: "${target.name}"
                           |Количество единиц: ${target.amount}
                           |Цена за единицу товара: ${target.price}
                           |Описание: ${target.description}
                           |_________________________________________
                           |""".stripMargin
                    }.getOrElse("Товар не найден. Выберите один из доступных товаров")
                )
                shopFlow()
            case "4" =>
                val orderId = customer.orders.length + 1
                customer = customer.copy(
                    orders = customer.orders :+ Order(orderId, List.empty, 0)
                )
                println(s"Для пользователя ${customer.session.username} Заказ №$orderId создан")
                shopFlow()
            case "5" =>
                println("Выберите номер заказа для добавления товара в него:")
                println(
                    customer.orders.map(order =>
                        s"[${order._id}] Заказ №${order._id}"
                    ).mkString("\n")
                )
                val orderId = StdIn.readLine()
                customer.orders.zipWithIndex.find(_._1._id == orderId.toInt) match {
                    case None =>
                        println("Заказ не найден. Выберите один из существующих")
                        shopFlow()
                    case Some((order, orderIndex)) =>
                        println("Введите название товара:")
                        val targetName = StdIn.readLine()
                        targets.get(targetName) match {
                            case None =>
                                println("Товар не найден. Введите название доступного товара")
                                shopFlow()
                            case Some(target) =>
                                println("Введите число единиц товара для добавления в заказ")
                                val amountToBuy = StdIn.readLong()
                                if (target.amount < amountToBuy) {
                                    println("Вы не можете добавить больше позиций товара, чем имеется. Добавьте товары заново")
                                    shopFlow()
                                } else {
                                    targets = targets - targetName + (
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
                                    customer = customer.copy(
                                        orders = customer.orders.updated(orderIndex, orderUpdated)
                                    )
                                    println("Единицы товара были добавлены в заказ")
                                    shopFlow()
                                }
                        }
                }
            case "6" =>
                println("Выберите номер заказа для его просмотра:")
                println(
                    customer.orders.map(order =>
                        s"[${order._id}] Заказ №${order._id}"
                    ).mkString("\n")
                )
                val orderId = StdIn.readLine()
                customer.orders.find(_._id == orderId.toInt) match {
                    case None =>
                        println("Заказ не найден. Выберите один из существующих")
                        shopFlow()
                    case Some(order) =>
                        println(
                            s"""
                               |Заказ №${order._id}
                               |Товары:
                               |     ${order.targets.map { target =>
                                s"""
                                   |Товар: "${target.name}"
                                   |Количество единиц: ${target.amount}
                                   |Цена за единицу товара: ${target.price}
                                   |Описание: ${target.description}
                                   |_________________________________________
                                   |""".stripMargin
                            }.mkString("\n")}
                               |Количество товаров: ${order.amount}
                               |Оплачено: ${if (order.payed) "Да" else "Нет"}
                            """.stripMargin
                        )
                        shopFlow()
                }
            case "7" =>
                println("Выберите номер заказа для его оплаты:")
                println(
                    customer.orders.map(order =>
                        s"[${order._id}] Заказ №${order._id}"
                    ).mkString("\n")
                )
                val orderId = StdIn.readLine()
                customer.orders.zipWithIndex.find(_._1._id == orderId.toInt) match {
                    case None =>
                        println("Заказ не найден. Выберите один из существующих")
                        shopFlow()
                    case Some((order, orderIndex)) =>
                        if (order.payed) {
                            println(s"Заказ №${orderId} уже был успешно оплачен")
                        } else {
                            val orderUpdated = order.copy(payed = true)
                            customer = customer.copy(
                                orders = customer.orders.updated(orderIndex, orderUpdated)
                            )
                            println(s"Заказ №${orderId} успешно оплачен")
                        }
                        shopFlow()
                }
            case "8" =>
                println("До свидания!")
            case _ =>
                println("Некорректный ввод. Введите цифру от 1 до 10")
                shopFlow()
        }
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


object DZ4 {

    trait Hash[DataType] {
        def getHash(data: DataType): Array[Byte]
    }

    case object PBKDF2WithHmacSHA1Hash extends Hash[Array[Char]] {
        override def getHash(data: Array[Char]): Array[Byte] = {
            HashApi.generateHash(data, HashApi.generateSalt)
        }
    }

    case object MD5Hash extends Hash[String] {
        override def getHash(data: String): Array[Byte] = {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(data.getBytes("utf-8"))
            val bigInt = new BigInteger(1, digest)
            bigInt.toByteArray
        }
    }

    def main(args: Array[String]): Unit = {
        println("*** Контрольное задание 4. Паттерны «шаблонный метод» и «стратегия» ***")
        println("Введите текст для хеширования:")
        val str = StdIn.readLine()
        println("Текст: " + str)
        println("Хеш, полученный с помощью алгоритма PBKDF2WithHmacSHA1:")
        println(new String(PBKDF2WithHmacSHA1Hash.getHash(str.toCharArray), "utf-8"))
        println("________________________________________________________")
        println("Текст: " + str)
        println("Хеш, полученный с помощью алгоритма MD5:")
        println(new String(MD5Hash.getHash(str), "utf-8"))
    }
}