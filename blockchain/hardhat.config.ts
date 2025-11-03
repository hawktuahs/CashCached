import { HardhatUserConfig } from "hardhat/config"
import "@nomicfoundation/hardhat-toolbox"
import "dotenv/config"

const rpcUrl = process.env.POLYGON_AMOY_RPC_URL || ""
const treasuryKey = process.env.TREASURY_PRIVATE_KEY || ""
const polygonScanKey = process.env.POLYGONSCAN_API_KEY || ""

const config: HardhatUserConfig = {
  solidity: "0.8.27",
  networks: {
    polygonAmoy: {
      url: rpcUrl,
      accounts: treasuryKey ? [treasuryKey] : []
    }
  },
  etherscan: {
    apiKey: {
      polygonAmoy: polygonScanKey
    }
  }
}

export default config
