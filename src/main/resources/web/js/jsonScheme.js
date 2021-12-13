var structuralUnitSchema = {
    "type": "object",
    "options": {
        "disable_properties": true,
        "remove_empty_properties": true
    },
    "properties": {
        "name": {
            "type": "string",
            "title": "Название",
            "options": {
                "remove_empty_properties": true
            }
        },
        "_id": {
            "type": "string",
            "title": "Идентификатор",
            "options": {
                "hidden": true,
                "remove_empty_properties": true
            }
        },
        "type": {
            "type": "string",
            "title": "Тип",
            "enum": ["BOOKKEEPING", "DEFAULT"],
            "propertyOrder": 1,
            "options": {
                "remove_empty_properties": true
            }
        },
        "purpose": {
            "type": "string",
            "title": "Назначение",
            "propertyOrder": 2
        },
        "staffTables": {
            "type": "array",
            "title": "Ставки работников подразделения",
            "propertyOrder": 3,
            "options": {
                "collapsed": true,
                "remove_empty_properties": true
            },
            "items": {
                "type": "object",
                "options": {
                    "collapsed": true,
                    "remove_empty_properties": true
                },
                "properties": {
                    "table": {
                        "type": "object",
                        "title": "Ставка",
                        "propertyOrder": 1,
                        "options": {
                            "collapsed": true,
                            "remove_empty_properties": true
                        },
                        "properties": {
                            "position": {
                                "type": "string",
                                "title": "Должность",
                                "propertyOrder": 1
                            },
                            "stakeRate": {
                                "type": "string",
                                "title": "Доля",
                                "propertyOrder": 2
                            }
                        }
                    },
                    "staff": {
                        "type": "array",
                        "title": "Работники",
                        "propertyOrder": 2,
                        "options": {
                            "collapsed": true,
                            "remove_empty_properties": true
                        },
                        "items": {
                            "type": "object",
                            "options": {
                                "collapsed": true,
                                "remove_empty_properties": true
                            },
                            "properties": {
                                "_id": {
                                    "type": "string",
                                    "title": "Идентификатор",
                                    "options": {
                                        "hidden": true,
                                        "remove_empty_properties": true
                                    }
                                },
                                "lastName": {
                                    "type": "string",
                                    "title": "Фамилия",
                                    "propertyOrder": 1
                                },
                                "firstName": {
                                    "type": "string",
                                    "title": "Имя",
                                    "propertyOrder": 2
                                },
                                "middleName": {
                                    "type": "string",
                                    "title": "Отчество",
                                    "propertyOrder": 3
                                },
                                "birthDate": {
                                    "type": "string",
                                    "title": "Дата рождения",
                                    "format": "date",
                                    "propertyOrder": 4
                                },
                                "stakeRates": {
                                    "type": "array",
                                    "title": "Ставки работника",
                                    "propertyOrder": 5,
                                    "options": {
                                        "collapsed": true,
                                        "remove_empty_properties": true
                                    },
                                    "items": {
                                        "type": "object",
                                        "options": {
                                            "collapsed": true,
                                            "remove_empty_properties": true
                                        },
                                        "properties": {
                                            "companyName": {
                                                "type": "string",
                                                "title": "Название подразделения",
                                                "propertyOrder": 1
                                            },
                                            "tables": {
                                                "type": "array",
                                                "title": "Ставки",
                                                "propertyOrder": 2,
                                                "options": {
                                                    "collapsed": true,
                                                    "remove_empty_properties": true
                                                },
                                                "items": {
                                                    "properties": {
                                                        "position": {
                                                            "type": "string",
                                                            "title": "Должность",
                                                            "propertyOrder": 1
                                                        },
                                                        "stakeRate": {
                                                            "type": "string",
                                                            "title": "Ставка",
                                                            "propertyOrder": 2
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                "employmentDate": {
                                    "type": "string",
                                    "title": "Дата приема на работу",
                                    "propertyOrder": 6
                                },
                                "passSeries": {
                                    "type": "string",
                                    "title": "Серия паспорта",
                                    "propertyOrder": 7
                                },
                                "passNumber": {
                                    "type": "string",
                                    "title": "Номер паспорта",
                                    "propertyOrder": 8
                                },
                                "snils": {
                                    "type": "string",
                                    "title": "СНИЛС",
                                    "propertyOrder": 9
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

var employeeSchema = {
    "type": "object",
    "options": {
        "disable_properties": true,
        "remove_empty_properties": true
    },
    "properties": {
        "_id": {
            "type": "string",
            "title": "Идентификатор",
            "options": {
                "hidden": true,
                "remove_empty_properties": true
            }
        },
        "lastName": {
            "type": "string",
            "title": "Фамилия",
            "propertyOrder": 1
        },
        "firstName": {
            "type": "string",
            "title": "Имя",
            "propertyOrder": 2
        },
        "middleName": {
            "type": "string",
            "title": "Отчество",
            "propertyOrder": 3
        },
        "birthDate": {
            "type": "string",
            "title": "Дата рождения",
            "format": "date",
            "propertyOrder": 4
        },
        "stakeRates": {
            "type": "array",
            "title": "Ставки работника",
            "propertyOrder": 5,
            "options": {
                "collapsed": true,
                "remove_empty_properties": true
            },
            "items": {
                "type": "object",
                "options": {
                    "collapsed": true,
                    "remove_empty_properties": true
                },
                "properties": {
                    "companyName": {
                        "type": "string",
                        "title": "Название подразделения",
                        "propertyOrder": 1
                    },
                    "tables": {
                        "type": "array",
                        "title": "Ставки",
                        "propertyOrder": 2,
                        "options": {
                            "collapsed": true,
                            "remove_empty_properties": true
                        },
                        "items": {
                            "properties": {
                                "position": {
                                    "type": "string",
                                    "title": "Должность",
                                    "propertyOrder": 1
                                },
                                "stakeRate": {
                                    "type": "string",
                                    "title": "Ставка",
                                    "propertyOrder": 2
                                }
                            }
                        }
                    }
                }
            }
        },
        "employmentDate": {
            "type": "string",
            "title": "Дата приема на работу",
            "propertyOrder": 6
        },
        "passSeries": {
            "type": "string",
            "title": "Серия паспорта",
            "propertyOrder": 7
        },
        "passNumber": {
            "type": "string",
            "title": "Номер паспорта",
            "propertyOrder": 8
        },
        "snils": {
            "type": "string",
            "title": "СНИЛС",
            "propertyOrder": 9
        }
    }
}