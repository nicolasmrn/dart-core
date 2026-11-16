package learn.core
 
import chisel3._
import chisel3.util._
import learn.core.fetch.FetchUnit
import learn.core.execute.ExecuteUnit
import learn.core.common.PipelineReg
 
class Top(params: CoreParams) extends Module {
  val io = IO(new Bundle {

   
    val i_bl    = Input(Bool())

   
    val i_addr  = Output(UInt(params.nAddrBit.W))
    val i_rdata = Input(UInt(params.nInstrBit.W))
 
   
    val d_addr  = Output(UInt(params.nDataBit.W))
    val d_wdata = Output(UInt(params.nDataBit.W))
    val d_we    = Output(Bool())
    val d_be    = Output(UInt(4.W))      
    val d_rdata = Input(UInt(params.nDataBit.W))
  })
 
  
  val fetch   = Module(new FetchUnit(params))
  val pipeReg = Module(new PipelineReg(params))
  val execute = Module(new ExecuteUnit(params))

  val global_stall = execute.io.stall || io.i_bl
 
 
  io.i_addr           := fetch.io.i_addr
  fetch.io.i_rdata    := io.i_rdata
  fetch.io.branchTaken  := execute.io.branchTaken
  fetch.io.branchTarget := execute.io.branchTarget
  fetch.io.stall := global_stall
 
 
  pipeReg.io.flush     := execute.io.flush || io.i_bl
  pipeReg.io.instr_in  := fetch.io.instr_out
  pipeReg.io.pc_val_in := fetch.io.pc_plus_one
  pipeReg.io.in_pc_current := fetch.io.pc_current
  pipeReg.io.stall := fetch.io.stall
 
  
  execute.io.instr       := pipeReg.io.instr_out
  execute.io.pc_plus_one := pipeReg.io.pc_val_out
  execute.io.pc_current := pipeReg.io.out_pc_current
 
 
  io.d_addr  := execute.io.d_addr
  io.d_wdata := execute.io.d_wdata
  io.d_we    := execute.io.d_we
  io.d_be    := execute.io.d_be
  execute.io.d_rdata := io.d_rdata
}