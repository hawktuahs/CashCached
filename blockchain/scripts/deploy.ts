import { ethers } from "hardhat"

async function main() {
  const owner = process.env.CASHCACHED_OWNER_ADDRESS
  if (!owner) {
    throw new Error("CASHCACHED_OWNER_ADDRESS is required")
  }
  const deployment = await ethers.deployContract("CashCached", [owner])
  await deployment.waitForDeployment()
  const address = await deployment.getAddress()
  console.log(`CashCached deployed at ${address}`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
