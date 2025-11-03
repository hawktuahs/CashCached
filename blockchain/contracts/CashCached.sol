// SPDX-License-Identifier: MIT
pragma solidity ^0.8.27;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract CashCached is ERC20, Ownable {
    constructor(address initialOwner) ERC20("CashCached", "CCHD") Ownable(initialOwner) {}

    function mint(address receiver, uint256 amount) external onlyOwner {
        _mint(receiver, amount);
    }

    function burnFromTreasury(uint256 amount) external onlyOwner {
        _burn(_msgSender(), amount);
    }

    function burnFrom(address holder, uint256 amount) external onlyOwner {
        _burn(holder, amount);
    }
}
