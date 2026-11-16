package learn.core.common

import chisel3._
import chisel3.util._
import learn.core.CoreParams

class PipelineReg(params: CoreParams) extends Module {
  val io = IO(new Bundle {
    val flush      = Input(Bool())          
    val stall           = Input(Bool())
    
   
    val instr_in   = Input(UInt(params.nInstrBit.W)) 
    val pc_val_in  = Input(UInt(params.nAddrBit.W))
    val in_pc_current   = Input(UInt(params.nAddrBit.W))
    
   
    val instr_out  = Output(UInt(params.nInstrBit.W)) 
    val pc_val_out = Output(UInt(params.nAddrBit.W))
    val out_pc_current  = Output(UInt(params.nAddrBit.W))
  })


  val instrReg = RegInit(0.U(params.nInstrBit.W))
  val pcReg    = RegInit(0.U(params.nAddrBit.W))
  val reg_pc_current  = RegInit(0.U(params.nAddrBit.W))

 
  when(io.flush) {
    instrReg := 0.U 
    pcReg    := 0.U
    reg_pc_current := 0.U
  } .elsewhen(io.stall) {
    instrReg       := instrReg
    pcReg          := pcReg
    reg_pc_current := reg_pc_current
  } .otherwise {
    instrReg := io.instr_in
    pcReg    := io.pc_val_in
    reg_pc_current := io.in_pc_current
  }

  
  io.instr_out  := instrReg
  io.pc_val_out := pcReg
  io.out_pc_current := reg_pc_current
}