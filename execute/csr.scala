package learn.core.execute

import chisel3._
import chisel3.util._
import learn.core.CoreParams

class CSR(params: CoreParams) extends Module {
  val io = IO(new Bundle {
    val csr_en = Input(Bool())
    val csr_addr  = Input(UInt(12.W)) 
    val csr_rdata = Output(UInt(params.nDataBit.W)) 
  })

  
  val mcycle_reg = RegInit(0.U(32.W))
  
  
  mcycle_reg := mcycle_reg + 1.U


  io.csr_rdata := Mux(io.csr_addr === "hB00".U && io.csr_en, mcycle_reg, 0.U)
}