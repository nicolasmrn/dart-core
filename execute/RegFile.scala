package learn.core.execute

import chisel3._
import chisel3.util._
import learn.core.Constants
import learn.core.CoreParams

class RegFile(params: CoreParams) extends Module {
  val io = IO(new Bundle {

    val rs1_addr = Input(UInt(Constants.REG_ADDR_W))
    val rs1_data = Output(UInt(params.nDataBit.W))
    
    val rs2_addr = Input(UInt(Constants.REG_ADDR_W))
    val rs2_data = Output(UInt(params.nDataBit.W))
    

    val wen      = Input(Bool())
    val rd_addr  = Input(UInt(Constants.REG_ADDR_W))
    val rd_data  = Input(UInt(params.nDataBit.W))
  })


  val regs = Reg(Vec(params.nGpr, UInt(params.nDataBit.W)))


  io.rs1_data := Mux(io.rs1_addr === 0.U, 0.U, regs(io.rs1_addr))
  io.rs2_data := Mux(io.rs2_addr === 0.U, 0.U, regs(io.rs2_addr))


  when(io.wen && io.rd_addr =/= 0.U) {
    regs(io.rd_addr) := io.rd_data
  }
}