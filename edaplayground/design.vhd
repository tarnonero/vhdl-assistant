library ieee;
use ieee.std_logic_1164.all;

entity half_adder is
    port(
    x, y, enable: IN BIT;
    carry, result: OUT BIT);
end half_adder ;

architecture structural of half_adder is
    component and2 
        port(
        in0, in1: IN BIT;
        out0: OUT BIT);
    end component;

    component xor2 
        port(
        in0, in1: IN BIT;
        out0: OUT BIT);
    end component;

    component and3 
        port(
        in0, in1, in2: IN BIT;
        out0: OUT BIT);
    end component;

    signal 
     xor_res: BIT;

    for all: and2  use entity work.and2 (behav);
    for all: xor2  use entity work.xor2 (behav);
    for all: and3  use entity work.and3 (behav);

    begin
        h0: and2  port map(in0 => enable,in1 => xor_res,out0 => result);
        h1: and3  port map(in0 => x,in1 => y,in2 => enable,out0 => carry);
        h2: xor2  port map(in0 => x,in1 => y,out0 => xor_res);
end structural;