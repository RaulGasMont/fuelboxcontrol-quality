package com.gasmonsoft.fbccalidad.utils.dataprocessing

import java.io.BufferedReader

class ReducirDatosSensor {
    data class Resultado(
        var fecha: MutableList<String> = mutableListOf(),
        var datos: MutableMap<Int, MutableList<Float>> = mutableMapOf()
    )

    companion object {
        fun reducir2(dOperar: Int, dDiesmar: Int, reader: BufferedReader): Resultado {
            val resultado = Resultado()
            val result = mutableMapOf<Int, MutableList<Float>>()
            val data = mutableMapOf<Int, MutableList<Float>>()
            val columns = mutableListOf<Int>()
            var contrDiesmar = 0
            var inicial = 0

            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split(" ")

                    if (inicial == 0) {
                        val tope = when (parts.size) {
                            5 -> 3
                            8 -> 7
                            11 -> 10
                            14 -> 13
                            else -> 0
                        }

                        for (i in 1..tope) {
                            columns.add(i)
                            result[i] = mutableListOf()
                            data[i] = mutableListOf()
                        }
                        inicial++
                    }

                    for (column in columns) {
                        if (column < parts.size) {
                            val value = parts[column].toFloat()
                            data[column]?.add(value)

                            if (data[column]!!.size > dOperar) {
                                data[column]?.removeAt(0)
                            }

                            if (contrDiesmar == 0) {
                                result[column]?.add(data[column]!!.average().toFloat())
                                if (column == 1) {
                                    resultado.fecha.add(parts[0])
                                }
                            }
                        }
                    }

                    if (contrDiesmar == 0) {
                        contrDiesmar++
                    } else {
                        if (contrDiesmar > dDiesmar) {
                            contrDiesmar = 0
                        } else {
                            contrDiesmar++
                        }
                    }
                }
                resultado.datos = result
            } catch (ex: Exception) {
                println("Se produjo un error: ${ex.message}")
            }

            return resultado
        }
    }
}