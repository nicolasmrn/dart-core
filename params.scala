package learn.core

import chisel3._
import chisel3.util._

// ******************************
//           PARAMETERS
// ******************************
trait CoreParams {
  def pcBoot: String
  def nInstrBit: Int = 32      // Largeur standard d'une instruction RISC-V
  def nAddrBit: Int            // Largeur du PC 
  def nDataBit: Int = 32       // Largeur des données
  def nDataByte: Int = (nDataBit / 8).toInt
  def useInstrReg: Boolean     // Présence du registre d'instruction (Pipeline 2 étages)
  def nGpr: Int                // Nombre de registres (16 pour RV32E) 
}

// ******************************
//             CONFIG
// ******************************
case class CoreConfig(
  pcBoot: String,
  nAddrBit: Int,
  useInstrReg: Boolean,
  nGpr: Int
) extends CoreParams

// ******************************
//             OBJECT
// ******************************
object CoreConfig {
  /**
    * Configuration par défaut pour le Dart-Core RV32E
    * - pcBoot: Adresse de démarrage (souvent 0x0)
    * - nAddrBit: 30 bits car les bits 1:0 sont fixés à 0 (alignement 32-bit) 
    * - useInstrReg: true pour isoler l'étage Fetch de l'étage Execute 
    * - nGpr: 16 registres (Spécification RV32E) 
    */
  def apply(
    pcBoot: String = "h00000000",
    nAddrBit: Int = 30,
    useInstrReg: Boolean = true,
    nGpr: Int = 16
  ): CoreConfig = new CoreConfig(
    pcBoot = pcBoot,
    nAddrBit = nAddrBit,
    useInstrReg = useInstrReg,
    nGpr = nGpr
  )

  
  def SmartCore: CoreParams = CoreConfig(
    pcBoot = "h00000000",
    nAddrBit = 30,
    useInstrReg = true,
    nGpr = 16
  )

}